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

  val filenameToSearchForRepos = argsOpt(0).getOrElse("../")
  val repoActivityLimit = argsOpt(1).getOrElse("10").toInt
  val sprintLengthInDays = argsOpt(2).getOrElse("14").toInt
  val displayLimit = argsOpt(3).getOrElse("4000").toInt

  def repositories(): Seq[VisibleRepo] = if (filenameToSearchForRepos == "--demo") {
    println("... using demodata / --demo 1")
    DemoData.get(sprintLengthInDays)
  } else {
    val repos = new File(filenameToSearchForRepos)
    print("... scanning for git dirs in " + repos.getAbsolutePath)
    def isGitDir(f: File): Boolean = f.isDirectory && f.getName == ".git"
    val inialFolderEntries = repos.listFiles()
    val repoDirs: Seq[File] = RepoAnalyzer.findRecursiv(inialFolderEntries, isGitDir).map(_.getAbsoluteFile)
    println(" ... done")
    if (repoDirs == Nil) {
      println("E: no repos found")
      Nil
    } else {
      val temp: Seq[VisibleRepo] = RepoAnalyzer.aggregate(repoDirs, sprintLengthInDays)
      temp
    }
  }

  val t1 = System.currentTimeMillis()
  new ReportGenerator(repositories()).write(sprintLengthInDays, displayLimit, repoActivityLimit)
  val t2 = System.currentTimeMillis()
  println("reports generated in " + (t2 - t1) + " (ms)")

}
