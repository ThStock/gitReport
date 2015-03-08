import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import ChangeTypes.{Contributor, VisibleChange, VisibleRepo}
import RepoAnalyzer._
import org.eclipse.jgit.api._
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.errors.{MissingObjectException, RevWalkException}
import org.eclipse.jgit.lib._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.revwalk.filter.{AndRevFilter, RevFilter, CommitTimeRevFilter}
import org.eclipse.jgit.storage.file._

import scala.collection.JavaConversions._

class RepoAnalyzer(repo: File, commitLimitDays: Long) {

  private val repository: Repository = new FileRepositoryBuilder().setGitDir(repo)
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
    val note = git.notesShow().setNotesRef("refs/notes/review")
      .setObjectId(commit).call()
    if (note == null) return Seq()
    val noteBytes = repository.open(note.getData).getCachedBytes
    val noteMessage = new String(noteBytes, "UTF-8")

    FooterElement.elementsIn(noteMessage)
  }

  private def toChange(commit: RevCommit): Option[Change] = {
    val authIden = commit.getAuthorIdent
    val footer: Seq[FooterElement] = commit
      .getFooterLines.map(e => FooterElement(e.getKey, e.getValue)) ++
      notesOf(commit)
    Some(Change(
      authIden.getEmailAddress,
      authIden.getName,
      commit.getFullMessage,
      commit.getId.abbreviate(7).name,
      commit.getCommitTime,
      footer
    ))
  }

  def changes(): Seq[Change] = {
    try {
      val logCmd = git.log()

      branchRefs
        .foreach(ref => logCmd.add(ref.getObjectId))

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

        val changes: List[Change] = changesOfWalk().toList.flatMap(toChange)
        changes
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
        .map(c => (c.authorName, c.authorEmail))
        .foldLeft(Map[String, String]())(_ + _)

      val result: Seq[VisibleChange] = allChanges.map(toVisChange(analy.toName(), authorsToEmails))
      new VisibleRepo(analy.name(), result, analy.branchNames(), commitLimitDays)
    }.seq

  }

  def toVisChange(repoName: String, authorsToEmails: Map[String, String])(change: Change): VisibleChange = {
    val authorKey: String = "author"
    val author = Contributor(change.authorEmail, authorKey)

    def lookup(username: String): String = authorsToEmails.getOrElse(username, username)

    val signers: Seq[Contributor] = filterAndMap(change.footer, "Signed-off-by", lookup)
    val reviewers: Seq[Contributor] = filterAndMap(change.footer, "Code-Review", lookup)

    if (signers != Nil) {
      val signerAuthor = Contributor(signers.head.email, authorKey)

      val signersWithoutFirst = signers.map(_.copy(typ = authorKey))
        .filterNot(_ == signerAuthor)

      VisibleChange(signerAuthor, Seq(author.copy(typ = "Code-Review")) ++ reviewers ++
        signersWithoutFirst, change.commitTime, repoName)

    } else {
      VisibleChange(author, reviewers ++ signers, change.commitTime, repoName)
    }

  }

  private def filterAndMap(footers: Seq[FooterElement], key: String, lookup: String => String): Seq[Contributor] = {
    footers
      .filter(_.key.startsWith(key))
      .map(foot => Contributor(foot.email.getOrElse(lookup(foot.value.trim)), foot.key))
  }

  def findRecursiv(files: Seq[File], filter: File => Boolean, matching: Seq[File] = Nil): Seq[File] = {
    val sub: Seq[File] = files.filter(filter)
    val singleMatch = sub.size == 1 && filter(sub(0))
    if (files == Nil || singleMatch) {
      matching ++ sub
    } else {
      findRecursiv(files.map(_.listFiles()).filterNot(_ == null).flatten, filter, sub ++ matching)
    }
  }

  def writeToFile(s: String, file: File) {
    Files.write(file.toPath, s.getBytes(StandardCharsets.UTF_8))
  }

  def md5(s: String) = java.security.MessageDigest.getInstance("MD5")
    .digest(s.getBytes).map("%02x".format(_)).mkString

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
      val elements: Seq[FooterElement] = lines
        .flatMap {
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
                    footer: Seq[FooterElement] = Seq()
                     )

}
