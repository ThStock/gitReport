import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.errors.{RevWalkException, MissingObjectException}

import scala.collection.JavaConversions._
import java.io._
import org.eclipse.jgit.storage.file._
import org.eclipse.jgit.lib._
import org.eclipse.jgit.api._
import org.eclipse.jgit.revwalk._
import RepoAnalyzer._

class RepoAnalyzer(repo:File, commitLimit:Int) {

  private val repository:Repository = new FileRepositoryBuilder().setGitDir(repo)
    .readEnvironment()
    .findGitDir()
    .build()

  private val git = new Git(repository)

  private lazy val branchRefs: Seq[Ref] = {
    git.branchList().call().filterNot(_.getName.contains("release"))
  }

  def branchNames():Seq[String] = {
    branchRefs.map(_.getName)
  }

  def name():String = toName()

  def toName(dir:File = repo):String = dir.getName match {
    case ".git" => toName(dir.getParentFile)
    case dirName => dirName
  }

  private def notesOf(commit:RevCommit):Seq[FooterElement] = {
    val note = git.notesShow().setNotesRef("refs/notes/review")
      .setObjectId(commit).call()
    if (note == null) return Seq()
    val noteBytes = repository.open(note.getData).getCachedBytes
    val noteMessage = new String(noteBytes,"UTF-8")

    FooterElement.elementsIn(noteMessage)
  }

  private def toChange(commit:RevCommit):Option[Change] = {
    val authIden = commit.getAuthorIdent
    val footer:Seq[FooterElement] = commit
      .getFooterLines.map(e => FooterElement(e.getKey, e.getValue)) ++
      notesOf(commit)
    Some(Change(
      authIden.getEmailAddress,
      commit.getFullMessage,
      commit.getId.abbreviate(7).name,
      commit.getCommitTime,
      footer
    ))
  }

  def changes():Seq[Change] = {
    try {
      val logCmd = git.log()

      branchRefs
        .foreach(ref => logCmd.add(ref.getObjectId))

      val changes:List[Change] = logCmd
        .setMaxCount(commitLimit)
        .call()
        .toList.flatMap(toChange)

      changes
    } catch {
      case e @ (_:MissingObjectException|_:NoHeadException|_:RevWalkException) => {
        System.err.println("E: skipping " + repo.getAbsolutePath + " " + e.getMessage)
        Seq()
      }
    }
  }
}

object RepoAnalyzer {

  def writeToFile( s: String, file:File) {
    val out = new PrintWriter(file, "UTF-8")
    try{ out.print( s ) }
      finally{ out.close() }
  }

  def md5(s: String) = java.security.MessageDigest.getInstance("MD5")
      .digest(s.getBytes).map("%02x".format(_)).mkString

  case class FooterElement(key:String, value:String) {
    lazy val email:Option[String] = {
      val stupidEmailPattern = "(.*) <(.*@.*)>".r
      // ^^ TODO
      value match {
        case stupidEmailPattern(_, e) => Some(e)
        case _ => None
      }
    }
  }

  object FooterElement {
    def elementsIn(text:String):Seq[FooterElement] = {
      val lines = text.split("\n")
      val footerPattern = "^([^ ]+):(.*)".r
      val elements:Seq[FooterElement] = lines
        .flatMap(line => line match {
            case footerPattern(key, value) => Some(FooterElement(key, value))
            case _ => None
          }
        )
      elements
    }
  }

  case class Change(
    authorEmail:String,
    commitMsg:String,
    id:String,
    commitTime:Int,
    footer:Seq[FooterElement] = Seq()
    )

}
