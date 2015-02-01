import java.io._
import RepoAnalyzer._
import java.util.Date
import ChangeTypes._

object Reporter extends App {
  /*
   * git config --add remote.origin.fetch +refs/notes/review:refs/notes/review
   * git config notes.displayRef refs/notes/review
   * git fetch
   */

  def argsOpt(arrayPosition: Int): Option[String] = arrayPosition match {
    case x if args.length > x => Some(args(x))
    case _ => None
  }

  val repos = new File(argsOpt(0).getOrElse("../"))
  val commitLimit = argsOpt(1).getOrElse("1200").toInt
  val displayLimit = argsOpt(2).getOrElse("700").toInt
  val repoActivityLimit = argsOpt(3).getOrElse("30").toInt


  def findRecursiv(files: Seq[File], filter: File => Boolean, matching:Seq[File] = Nil): Seq[File] = {
    val sub:Seq[File] = files.filter(filter)
    val singleMatch = sub.size == 1 && filter(sub(0))
    if (files == Nil || singleMatch) {
      matching
    } else {
      findRecursiv(files.map(_.listFiles()).filterNot(_ == null).flatten, filter, sub ++ matching)
    }
  }

  print("... scanning for git dirs")

  private def isGitDir(f: File): Boolean = f.isDirectory && f.getName == ".git"

  val repoDirs: Seq[File] = findRecursiv(repos.listFiles(), isGitDir).map(_.getAbsoluteFile)
  println(" ... done")
  val t1 = System.currentTimeMillis()

  val changes:Seq[VisibleRepo] = repoDirs.par.map { repo =>
      println("Scanning:   " + repo)
      val analy = new RepoAnalyzer(repo, commitLimit)
      val allChanges: Seq[Change] = analy.changes()
      def toVisChange(repoName: String)(change: Change): VisibleChange = {
        val author = Contributor(change.authorEmail, "author")
        // TODO handle SignedOfBy
        val reviewers: Seq[FooterElement] = change.footer
          .filter(_.key.startsWith("Code-Review"))

        val contribs: Seq[Contributor] = reviewers
          .map(foot => Contributor(foot.email.getOrElse(foot.value), foot.key))
        VisibleChange(author, contribs, change.commitTime, repoName)
      }

      val result: Seq[VisibleChange] = allChanges.map(toVisChange(analy.toName()))
      new VisibleRepo(analy.name(), result, analy.branchNames())
    }.seq


  val t2 = System.currentTimeMillis()
  new ReportGenerator(changes).write(displayLimit, repoActivityLimit)
  println("reports generated in " + (t2 - t1) + " (ms)")
}
