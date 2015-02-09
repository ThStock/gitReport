import java.io.{File, InputStream}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import ChangeTypes._
import com.gilt.handlebars.scala.Handlebars
import com.gilt.handlebars.scala.binding.dynamic._

class ReportGenerator(repos: Seq[VisibleRepo]) {

  def write(displayLimit: Int, repoActivityLimit: Int) {
    val content: Seq[VisibleChange] = repos.flatMap(_.changes)
      .sortBy(_.commitTime).reverse

    writeByName("truckMap", content.take(displayLimit))

    writeTruckByRepo(repoActivityLimit, content, displayLimit)
  }

  def writeTruckByRepo(repoActivityLimit: Int, content: Seq[VisibleChange], displayLimit: Int) {
    val repoByName = repos.groupBy(_.repoName)
    def branchNamesOf(key: String) = repoByName.get(key).get.head.branchNames

    val latestCommitDate = content.map(_.commitTime).max

    def write(dayDelta: Int) {

      val contentGrouped = content
        .filter(_.commitTime < latestCommitDate - dayDelta * 86400)
        .take(displayLimit)
        .groupBy(_.repoName)

      val truckByProject: Seq[VisibleRepo] = contentGrouped.toSeq
        .map(in => VisibleRepo(in._1, in._2, branchNamesOf(in._1), scoreOf(in._1, repoActivityLimit, contentGrouped)))
        .filter(_.changes.size > repoActivityLimit)
        .sortBy(_.repoName).sortWith(_.percentageOk > _.percentageOk)

      writeByName("truckByProject", truckByProject, "truckByProject" + dayDelta)

    }

    Range(0, 5).foreach(write)

    copyToOutput("octoicons/octicons.css")
    copyToOutput("octoicons/octicons.eot")
    copyToOutput("octoicons/octicons.svg")
    copyToOutput("octoicons/octicons.woff")
  }

  case class RepoGroup(amount: Int)

  case class RepoStatus(repoNamesToChangeCount: Map[String, Int], repoNameToSize: Seq[Int]) {
    val min = RepoGroup(repoNameToSize(0))
    val mid = RepoGroup(repoNameToSize(1))
    val max = RepoGroup(repoNameToSize(2))

    def changeCountOf(repoName: String) = repoNamesToChangeCount.get(repoName).get
  }

  def scoreOf(repoName: String, repoActivityLimit: Int, contentGrouped: Map[String, Seq[VisibleChange]]): Int = {
    val repoStat = repoStatus(repoActivityLimit, contentGrouped)
    repoStat.changeCountOf(repoName) match {
      case i: Int if i <= repoStat.min.amount => 0
      case i: Int if i < repoStat.mid.amount => 1
      case i: Int if i >= repoStat.mid.amount => 2
      case _ => 1
    }
  }

  def repoStatus(repoActivityLimit: Int, repoNameToChanges: Map[String, Seq[VisibleChange]]): RepoStatus = {
    val repoNameToChangeCount = repoNameToChanges.map(in => (in._1, in._2.size))
    val repoActivities = repoNameToChangeCount.map(_._2).filter(_ > repoActivityLimit).toSeq.sorted
    val repoSlideCount = 3
    val repoSegementSize: Int = repoActivities.size / repoSlideCount
    val repoSlides: Seq[Seq[Int]] = repoActivities.sliding(repoSegementSize, repoSegementSize).toList

    val repoSlidesFixedSize: Seq[Seq[Int]] = repoSlides match {
      case s4 if s4.size >= 4 => Seq(s4(0), s4(1), s4.takeRight(s4.size - 2).flatten)
      case s3 if s3.size == 3 => Seq(s3(0), s3(1), s3(2))
      case any => throw new IllegalStateException("wrong sliding " + any.size)
    }

    RepoStatus(repoNameToChangeCount, repoNameToSize = repoSlidesFixedSize.map(_.max))
  }

  private lazy val outDir: File = {
    val out = new File("out")
    if (!out.isDirectory) {
      out.mkdir()
    }
    out
  }

  private def copyToOutput(path: String) {
    val outputFilename = outDir.toPath.resolve(path)
    val parant = outputFilename.getParent
    if (!Files.isDirectory(parant)) {
      Files.createDirectories(parant)
    }
    if (!Files.exists(outputFilename)) {
      Files.copy(ReportGenerator.fileFrom(path), outputFilename)
    }
  }

  private def writeByName(reportFileName: String, content: Any, outputFileName: String = "") {
    val fileName = reportFileName + ".mu"

    val text = io.Source.fromInputStream(ReportGenerator.fileFrom(fileName)).mkString
    val template = Handlebars(text)

    val contentMap: Map[String, Any] = Map(//
      "content" -> content,
      "reportDate" -> ReportGenerator.formatedDate(new Date())
    )
    val outFileNameWithSuffix = if (outputFileName.isEmpty) {
      reportFileName
    } else {
      outputFileName
    } + ".html"

    val outputFile = new File(outDir, outFileNameWithSuffix)
    RepoAnalyzer.writeToFile(template(contentMap), outputFile)
    println("written: " + outputFile.getAbsolutePath)
  }

}

object ReportGenerator {

  private def fileFrom(fileName: String): InputStream = {
    getClass.getResourceAsStream(fileName)
  }

  def formatedDate(date: Date): String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date)
  }
}
