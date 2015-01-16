import scala.collection.JavaConversions._
import java.io._
import org.eclipse.jgit.storage.file._
import org.eclipse.jgit.lib._
import org.eclipse.jgit.api._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.diff._
import org.eclipse.jgit.util.io._
import RepoAnalyzer._

class RepoAnalyzer(repo:File, commitLimit:Int) {

  val repository:Repository = new FileRepositoryBuilder().setGitDir(repo)
    .readEnvironment()
    .findGitDir()
    .build()

  def name(dir:File = repo):String = dir.getName match {
    case ".git" => name(dir.getParentFile)
    case dirName => dirName
  }

  def getChanges():Seq[Change] = {

    val git = new Git(repository)

    def notesOf(commit:RevCommit):Seq[FooterElement] = {
      val note = git.notesShow().setNotesRef("refs/notes/review")
        .setObjectId(commit).call()
      if (note == null) return Seq()
      val noteBytes = repository.open(note.getData()).getCachedBytes()
      val noteMessage = new String(noteBytes,"UTF-8")

      return FooterElement.elementsIn(noteMessage)
    }

    def toChange(commit:RevCommit):Option[Change] = {
      val authIden = commit.getAuthorIdent()
      val footer:Seq[FooterElement] = commit
        .getFooterLines.map(e => FooterElement(e.getKey, e.getValue)) ++
         notesOf(commit)
      return Some(Change(
          authIden.getEmailAddress,
          commit.getFullMessage,
          commit.getId.abbreviate(7).name,
          commit.getCommitTime,
          footer
          ))
    }

    try {
      val branches = git.branchList()
      // TODO ListMode
      .call()
      val logCmd = git.log()
      branches.foreach(ref => logCmd.add(ref.getObjectId))
      val changes:List[Change] = logCmd
        .setMaxCount(commitLimit)
        .call()
        .toList.flatMap(toChange)

      return changes
    } catch {
      case _:org.eclipse.jgit.api.errors.NoHeadException => return Seq()
    }
  }
}

object RepoAnalyzer {

  def writeToFile( s: String, file:File) {
    val out = new PrintWriter(file, "UTF-8")
    try{ out.print( s ) }
      finally{ out.close }
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
      return elements
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
