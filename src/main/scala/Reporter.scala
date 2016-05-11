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

  val defaultScanPath = "../"
  val defaultRepoActivityLimit = "10"
  val defaultSprintLengthInDays = "14"
  val defaultDisplayLimit = "4000"

  val filenameToSearchForRepos = argsOpt(0).getOrElse(defaultScanPath)
  val repoActivityLimit = argsOpt(1).getOrElse(defaultRepoActivityLimit).toInt
  val sprintLengthInDays = argsOpt(2).getOrElse(defaultSprintLengthInDays).toInt
  val displayLimit = argsOpt(3).getOrElse(defaultDisplayLimit).toInt

  def repositories(): Seq[VisibleRepo] = if (filenameToSearchForRepos == "--demo") {
    println("... using demodata / --demo 1")
    DemoData.get(sprintLengthInDays)
  } else if (filenameToSearchForRepos == "--help") {
    println("")
    println("Usage: %s [Path to Scan] [repoActivityLimit] [sprintLengthInDays] [displayLimit]".format(selfJar))
    println("     : %s --help".format(selfJar))
    println("     : %s --demo [repoActivityLimit] [sprintLengthInDays] [displayLimit]".format(selfJar))
    println("")
    println("  Params:")
    println("    Path to Scan; default is %s".format(defaultScanPath))
    println("    repoActivityLimit; default is %s".format(defaultRepoActivityLimit))
    println("    sprintLengthInDays; default is %s".format(defaultSprintLengthInDays))
    println("    displayLimit; default is %s".format(defaultDisplayLimit))
    println("")
    System.exit(1)
    Nil
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

  private lazy val selfJar:String = {
    val potentialJar = new File(this.getClass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
      .getName
    if (potentialJar == "classes") {
      "."
    } else {
      potentialJar
    }
  }
}
