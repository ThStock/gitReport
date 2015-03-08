import java.io._

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
  val commitLimitDays = argsOpt(1).getOrElse("30").toInt
  val displayLimit = argsOpt(2).getOrElse("4000").toInt
  val repoActivityLimitInDays = argsOpt(3).getOrElse("10").toInt

  print("... scanning for git dirs in " + repos.getAbsolutePath)

  private def isGitDir(f: File): Boolean = f.isDirectory && f.getName == ".git"
  val inialFolderEntries = repos.listFiles()
  val repoDirs: Seq[File] = RepoAnalyzer.findRecursiv(inialFolderEntries, isGitDir).map(_.getAbsoluteFile)
  println(" ... done")
  if (repoDirs == Nil) {
    println("E: no repos found")
  } else {
    val t1 = System.currentTimeMillis()

    val changes: Seq[VisibleRepo] = RepoAnalyzer.aggregate(repoDirs, commitLimitDays)

    val t2 = System.currentTimeMillis()
    new ReportGenerator(changes).write(displayLimit, repoActivityLimitInDays)
    println("reports generated in " + (t2 - t1) + " (ms)")

  }
}
