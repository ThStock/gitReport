import java.io._

import ChangeTypes._
import RepoAnalyzer._

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

  print("... scanning for git dirs")

  private def isGitDir(f: File): Boolean = f.isDirectory && f.getName == ".git"

  val repoDirs: Seq[File] = RepoAnalyzer.findRecursiv(repos.listFiles(), isGitDir).map(_.getAbsoluteFile)
  println(" ... done")
  val t1 = System.currentTimeMillis()

  val changes: Seq[VisibleRepo] = RepoAnalyzer.aggregate(repoDirs, commitLimit)

  val t2 = System.currentTimeMillis()
  new ReportGenerator(changes).write(displayLimit, repoActivityLimit)
  println("reports generated in " + (t2 - t1) + " (ms)")
}
