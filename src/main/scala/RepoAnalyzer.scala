import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import ChangeTypes.{Contributor, ContributorType, VisibleChange, VisibleRepo}
import RepoAnalyzer._
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.errors.{MissingObjectException, RevWalkException}
import org.eclipse.jgit.lib._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.revwalk.filter.{AndRevFilter, CommitTimeRevFilter, RevFilter}
import org.eclipse.jgit.storage.file._

import scala.collection.JavaConversions._
import scala.collection.parallel.ParSeq

class RepoAnalyzer(repo: File, commitLimitDays: Long) {

  private val repository: Repository = new FileRepositoryBuilder()
    .setGitDir(repo)
    .readEnvironment()
    .findGitDir()
    .build()

  private val git = new Git(repository)

  private lazy val branchRefs: Seq[Ref] = {
    git.branchList().call().filterNot(_.getName.contains("release"))
  }

  def branchNames(): Seq[String] = {
    branchRefs.map(_.getName)
  }

  def name(): String = toName()

  def toName(dir: File = repo): String = dir.getName match {
    case ".git" => toName(dir.getParentFile)
    case "." => toName(dir.getParentFile)
    case dirName => dirName
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
                 commit.getCommitTime,
                 footerElements,
                 config.highlightPersonalExchange))
  }

  def changes(): Seq[Change] = {
    try {
      val logCmd = git.log()

      branchRefs.foreach(ref => logCmd.add(ref.getObjectId))

      val walk = new RevWalk(git.getRepository)
      val headId = git.getRepository.resolve(Constants.HEAD)
      if (headId == null) {
        System.err.println("E: skipping " + repo.getAbsolutePath + " no HEAD")
        Nil
      } else {
        val now = System.currentTimeMillis()

        val timeFilter = CommitTimeRevFilter.between(now - 86400000L * commitLimitDays, now)
        walk.setRevFilter(AndRevFilter.create(RevFilter.NO_MERGES, timeFilter))
        val root = walk.parseCommit(headId)
        walk.markStart(root)

        def changesOfWalk(): Seq[RevCommit] = {
          val change = walk.next()
          if (change != null) {
            Seq(change) ++ changesOfWalk()
          } else {
            walk.release()
            Nil
          }
        }
        val configFile = new File(repo.getParentFile.getAbsoluteFile, ".git-report.conf")
        val conf = RepoConfig(ConfigFactory.parseFile(configFile))
        def toChangeFn(commit: RevCommit) = toChange(conf, commit)
        changesOfWalk().toList.flatMap(toChangeFn)
      }
    } catch {
      case e@(_: MissingObjectException | _: NoHeadException | _: RevWalkException | _: NullPointerException) => {
        System.err.println("E: skipping " + repo.getAbsolutePath + " " + e.getMessage)
        Nil
      }
    }
  }
}

object RepoAnalyzer {

  def aggregate(repoDirs: Seq[File], commitLimitDays: Int): Seq[VisibleRepo] = {
    repoDirs.par.map { repo =>
      println("Scanning:   " + repo)
      val analy = new RepoAnalyzer(repo, commitLimitDays)
      val allChanges: Seq[Change] = analy.changes()
      val authorsToEmails: Map[String, String] = allChanges //
        .map(c => (c.authorName, c.authorEmail)).foldLeft(Map[String, String]())(_ + _)

      val result: Seq[VisibleChange] = allChanges.map(toVisChange(analy.toName(), authorsToEmails))
      new VisibleRepo(analy.name(), result, analy.branchNames(), commitLimitDays)
    }.seq

  }

  def toVisChange(repoName: String, authorsToEmails: Map[String, String])(change: Change): VisibleChange = {
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

      VisibleChange(signerAuthor,
                     Seq(author.copy(_typ = Contributor.REVIWER, email = author.email.toLowerCase)) ++ reviewers ++
                       signersWithoutFirst,
                     change.commitTime,
                     repoName,
                     change.highlightPersonalExchange)

    } else {
      VisibleChange(author.copy(email = author.email.toLowerCase),
                     reviewers ++ signers,
                     change.commitTime,
                     repoName,
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
                     visChange.commitTime, visChange.repoName, change.highlightPersonalExchange)
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
                    commitTime: Int,
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
