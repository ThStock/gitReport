import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import ChangeTypes.{Contributor, ContributorType, VisibleChange, VisibleRepo}
import RepoAnalyzer._
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.lib._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.revwalk.filter.{AndRevFilter, CommitTimeRevFilter, RevFilter}
import org.eclipse.jgit.storage.file._
import resource._

import scala.collection.JavaConversions._
import scala.collection.parallel.ParSeq

class RepoAnalyzer(repo: File, commitLimitMillis: Long) {

  private val repository: Repository = new FileRepositoryBuilder() //
    .setGitDir(repo) //
    .readEnvironment() //
    .findGitDir() //
    .build()

  private val git = new Git(repository)

  private lazy val branchRefs: Seq[Ref] = {
    git.branchList().call().filterNot(_.getName.contains("release"))
  }

  def branchNames(): Seq[String] = {
    branchRefs.map(_.getName)
  }

  def name(): String = toName()

  def absolutPath(): String = toFolder().getAbsolutePath // TODO TEST
  def toName(dir: File = repo): String = toFolder(dir).getName

  def toFolder(dir: File = repo): File = dir.getName match {
    case ".git" => toFolder(dir.getParentFile)
    case "." => toFolder(dir.getParentFile)
    case dirName => dir
  }

  private def notesOf(commit: RevCommit): Seq[FooterElement] = {
    val note = git.notesShow().setNotesRef("refs/notes/review").setObjectId(commit).call()
    if (note == null) return Seq()
    val noteBytes = repository.open(note.getData).getCachedBytes
    val noteMessage = new String(noteBytes, "UTF-8")

    FooterElement.elementsIn(noteMessage)
  }

  private def toChange(config: RepoConfig, commit: RevCommit): Option[Change] = {
    val authIden = commit.getAuthorIdent
    val footerElements: Seq[FooterElement] = if (config.highlightGerritActivity) {
      commit.getFooterLines.map(e => FooterElement(e.getKey, e.getValue)) ++ notesOf(commit)
    } else {
      Nil
    }

    Some(Change(authIden.getEmailAddress,
                 authIden.getName,
                 commit.getFullMessage,
                 commit.getId.abbreviate(7).name,
                 commit.getCommitTime * 1000L,
                 footerElements,
                 config.highlightPersonalExchange))
  }

  def changes(participationBarCount:Int): Seq[Change] = {
    def logSkipMessage(msg:String) = {
      System.err.println("W: skipping " + repo.getAbsolutePath + " " + msg)
    }
    try {
      val headId = git.getRepository.resolve(Constants.HEAD)
      if (headId == null) {
        logSkipMessage("no HEAD")
        Nil
      } else {
        val now = System.currentTimeMillis()

        val timeFilter = CommitTimeRevFilter.between(now - 86400000L * (commitLimitMillis * participationBarCount), now)
        implicit def connectionResource[A <: RevWalk] = new Resource[A] {
          override def close(r: A) = r.dispose()
          override def closeAfterException(r: A, t: Throwable): Unit = {
            logSkipMessage("resource " + t.getMessage)
            close(r)
          }
        }
        val configFile = new File(repo.getParentFile.getAbsoluteFile, ".git-report.conf")
        val conf = RepoConfig(ConfigFactory.parseFile(configFile))

        def toChangeFn(commit: RevCommit) = toChange(conf, commit)

        resource.managed(new RevWalk(git.getRepository)).map { walk: RevWalk =>
          walk.setRevFilter(AndRevFilter.create(RevFilter.NO_MERGES, timeFilter))
          val root = walk.parseCommit(headId)
          walk.markStart(root)
          def changesOfWalk(): Seq[RevCommit] = {
            val change = walk.next()
            if (change != null) {
              Seq(change) ++ changesOfWalk()
            } else {
              Nil
            }
          }
          changesOfWalk().flatMap(toChangeFn)
        }.opt.orElse(Some(Nil)).get
      }
    } catch {
      case e@(_: MissingObjectException | _: NoHeadException  | _: NullPointerException) => {
        logSkipMessage(e.getMessage)
        Nil
      }
    }
  }
}

object RepoAnalyzer {

  def aggregate(repoDirs: Seq[File], commitLimitDays: Int): Seq[VisibleRepo] = {

    val commitLimitMillis = 86400000L * commitLimitDays
    repoDirs.par.map { repo =>
      println("Scanning:   " + repo)
      val participationBarCount = 19
      val analy = new RepoAnalyzer(repo, commitLimitMillis)
      val allChanges: Seq[Change] = analy.changes(participationBarCount)
      val barPercentages = calcParticipationPercentages(allChanges.map(_.commitTimeMillis), participationBarCount, //
        commitLimitMillis, System.currentTimeMillis())
      val now = System.currentTimeMillis()
      val relevantChanges = allChanges.filter(c ⇒ c.commitTimeMillis >= now - commitLimitMillis)
      val authorsToEmails: Map[String, String] = relevantChanges //
        .map(c => (c.authorName, c.authorEmail)).foldLeft(Map[String, String]())(_ + _)

      val result: Seq[VisibleChange] = relevantChanges.map(toVisChange(analy.toName(), analy.absolutPath(), authorsToEmails))

      new VisibleRepo(
        repoName = analy.name(),
        repoFullPath = analy.toFolder(repo).getAbsolutePath,
        _changes = result,
        branchNames = analy.branchNames(),
        _sprintLengthInDays = commitLimitDays,
        participationPercentages = barPercentages)
    }.seq

  }

  def calcParticipationPercentages(timeStampsOfAllCommits: Seq[Long], barCount: Int, windowMillis: Long, nowMillis:Long): Seq[Int] = {
    val x = nowMillis / windowMillis
    val groupedTimesByWindow = timeStampsOfAllCommits.groupBy(in ⇒ ((in / windowMillis) - x).abs).map(in ⇒ (in._1, in._2.size))
    val maxCommitsPerWindow = if (groupedTimesByWindow.isEmpty) {
      0
    } else {
      groupedTimesByWindow.values.max
    }
    val percentages = groupedTimesByWindow.map(in ⇒ (in._1, (in._2.toDouble / maxCommitsPerWindow) * 100d))
    Seq.tabulate(barCount)(i ⇒ percentages.getOrElse(i, 0d).toInt)
  }

  def toVisChange(repoName: String, absolutRepoPath: String, authorsToEmails: Map[String, String]) //
                 (change: Change): VisibleChange = {
    val author = Contributor(change.authorEmail, Contributor.AUTHOR)

    def lookup(username: String): String = {
      val email = authorsToEmails.get(username)
      if (email.isDefined) {
        email.get.toLowerCase
      } else {
        username
      }
    }

    val signers: Seq[Contributor] = filterAndMap(change.footer, "Signed-off-by", lookup)
    val reviewers: Seq[Contributor] = filterAndMap(change.footer, "Code-Review", lookup)

    val visChange = if (signers != Nil) {
      val signerAuthor = Contributor(signers.head.email.toLowerCase, Contributor.AUTHOR)
      val signersWithoutFirst = signers.map(_.copy(_typ = Contributor.AUTHOR)).filterNot(_ == signerAuthor)

      def noSigner(contributor: Contributor) = signers.map(_.email).contains(contributor.email)

      VisibleChange(signerAuthor, (Seq(author.copy(_typ = Contributor.REVIWER, email = author.email.toLowerCase)) ++
        reviewers).filterNot(noSigner) ++
        signersWithoutFirst, change.commitTimeMillis, repoName, absolutRepoPath, change.highlightPersonalExchange)

    } else {
      VisibleChange(author.copy(email = author.email.toLowerCase),
                     reviewers ++ signers,
                     change.commitTimeMillis,
                     repoName,
                     absolutRepoPath,
                     change.highlightPersonalExchange)
    }

    if (change.highlightPersonalExchange) {
      visChange
    } else {
      val reviewer = if (visChange.contributors.map(_.hash).contains(author.hash)) {
        "some@example.org"
      } else {
        "other@example.org"
      }
      VisibleChange(Contributor("some@example.org", Contributor.AUTHOR), //
                     visChange.contributors.map(_.copy(email = reviewer)), //
                     visChange.commitTimeMillis, visChange.repoName, visChange.repoFullPath, change.highlightPersonalExchange)
    }
  }

  private def filterAndMap(footers: Seq[FooterElement], key: String, lookup: String => String): Seq[Contributor] = {
    footers.filter(_.key.startsWith(key)).map(foot => {
      val emailOrName = if (foot.email.isDefined) {
        foot.email.get.toLowerCase
      } else {
        lookup(foot.value.trim)
      }
      Contributor(emailOrName, ContributorType(foot.key))
    })
  }

  def findRecursiv(files: Seq[File], filter: File => Boolean): Seq[File] = {
    def parFindRecursiv(parFiles: ParSeq[File], matching: ParSeq[File] = Nil.par): ParSeq[File] = {
      val sub: ParSeq[File] = parFiles.filter(filter)
      def isSingleMatch = sub.size == 1 && filter(sub.head)
      if (parFiles == Nil.par || isSingleMatch) {
        matching ++ sub
      } else {
        parFindRecursiv(parFiles.map(_.listFiles()).filterNot(_ == null).flatten, sub ++ matching)
      }
    }
    parFindRecursiv(files.par).seq
  }

  def writeToFile(s: String, file: File) {
    Files.write(file.toPath, s.getBytes(StandardCharsets.UTF_8))
  }

  def md5(s: String) = java.security.MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02x".format(_)).mkString

  case class FooterElement(key: String, value: String) {
    lazy val email: Option[String] = {
      val stupidEmailPattern = "(.*) <(.*@.*)>".r
      // ^^ TODO
      value match {
        case stupidEmailPattern(_, e) => Some(e)
        case _ => None
      }
    }
  }

  object FooterElement {

    def signer(name:String, email:String) = FooterElement("Signed-off-by", name + " <" + email + ">")

    def elementsIn(text: String): Seq[FooterElement] = {
      val lines = text.split("\n")
      val footerPattern = "^([^ ]+):(.*)".r
      val elements: Seq[FooterElement] = lines.flatMap {
        //
        case footerPattern(key, value) => Some(FooterElement(key, value.trim))
        case _ => None
      }
      elements
    }
  }

  case class Change(authorEmail: String,
                    authorName: String,
                    commitMsg: String,
                    id: String,
                    commitTimeMillis: Long,
                    footer: Seq[FooterElement] = Nil,
                    highlightPersonalExchange: Boolean = false)

  sealed case class RepoConfig(config: com.typesafe.config.Config) {

    val isValid = try {
      config.checkValid(ConfigFactory.defaultReference(), "main")
      true
    } catch {
      case _: ConfigException => false
    }

    private val fallbackConfig = config.withFallback(ConfigFactory.defaultReference())

    val highlightGerritActivity = fallbackConfig.getBoolean("main.gerrit.higlightReviewExchange")

    val highlightPersonalExchange = fallbackConfig.getBoolean("main.gerrit.higlightPersonalExchange")
  }

}
