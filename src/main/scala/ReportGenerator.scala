import java.io.{File, InputStream}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import ChangeTypes._
import com.gilt.handlebars.scala.Handlebars
import com.gilt.handlebars.scala.binding.dynamic._

class ReportGenerator(repos: Seq[VisibleRepo]) {

  def write(commitLimitDays: Int, displayLimit: Int, repoActivityLimitInDays: Int) {
    if (repos != Nil) {
      val content: Seq[VisibleChange] = repos.flatMap(_.changes)
        .sortBy(_.commitTime).reverse

      writeByName("truckMap", content.take(displayLimit))

      writeTruckByRepo(repoActivityLimitInDays, content, displayLimit, commitLimitDays)
    }
  }

  private def writeTruckByRepo(repoActivityLimitInDays: Int, content: Seq[VisibleChange], displayLimit: Int, commitLimitDays: Int) {
    val repoByName = repos.groupBy(_.repoName)
    def branchNamesOf(key: String) = repoByName.get(key).get.head.branchNames

    val latestCommitDate = content.map(_.commitTime).max.toLong

    def write(dayDelta: Int) {

      val filterCommitDate = latestCommitDate - dayDelta * 86400L
      val contentListed = content
        .filter(_.commitTime < filterCommitDate)
        .take(displayLimit)
      if (contentListed != Nil) {
        val contentGrouped = contentListed
          .groupBy(_.repoName)

        val truckByProject: Seq[VisibleRepo] = contentGrouped.toSeq
          .map(in => VisibleRepo(in._1, in._2, branchNamesOf(in._1), commitLimitDays, scoreOf(in._1, repoActivityLimitInDays, contentGrouped)))
          .filter(_.changes.size > repoActivityLimitInDays)
          .sortBy(_.repoName).sortWith(_.percentageOk > _.percentageOk)

        if (truckByProject == Nil) {
          println("W: no repos will appear in report")
        }

        case class Slot(repos: Seq[VisibleRepo])

        case class Segmented(slots: Seq[Slot], newestCommitDate: String, latestCommitDate: String)
        val segments = ReportGenerator.slidingsOf(3)(truckByProject)

        val segemnts = Segmented(slots = Seq(Slot(segments(0)), Slot(segments(1)), Slot(segments(2))),
          latestCommitDate = ReportGenerator.formatedDateBySecs(contentListed.map(_.commitTime).min),
          newestCommitDate = ReportGenerator.formatedDateBySecs(filterCommitDate)
        )
        writeByName("truckByProject", segemnts, "truckByProject" + dayDelta)

      }
    }

    Range(0, 5).foreach(write)

    copyToOutput("octoicons/octicons.css")
    copyToOutput("octoicons/octicons.eot")
    copyToOutput("octoicons/octicons.svg")
    copyToOutput("octoicons/octicons.woff")
    copyToOutput("bootstrap-3.3.2-dist/css/bootstrap.min.css")
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
    val repoSlidings: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(repoActivities)
    def maxIfPresent(in: Seq[Int]): Int = if (in == Nil) {
      0
    } else {
      in.max
    }
    RepoStatus(repoNameToChangeCount, repoNameToSize = repoSlidings.map(maxIfPresent))
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
    }

    val outputFile = new File(outDir, outFileNameWithSuffix + ".html")
    RepoAnalyzer.writeToFile(template(contentMap), outputFile)
    println("written: " + outputFile.getAbsolutePath)
  }

}

object ReportGenerator {

  def slidingsOf[A](maxLength: Int)(sortedIn: Seq[A]): Seq[Seq[A]] = {
    val slots: Int = sortedIn.size / maxLength

    if (sortedIn.length < maxLength) {
      val toFill = (maxLength - sortedIn.length) / 2
      val rightFill = toFill + toFill + sortedIn.length match {
        case a if a < maxLength => toFill + 1
        case _ => toFill
      }

      Seq.fill(maxLength)(Nil).take(toFill) ++ sortedIn.map(Seq(_)) ++ Seq.fill(maxLength)(Nil).take(rightFill)
    } else {
      val slidings: Seq[Seq[A]] = sortedIn.sliding(slots, slots).toList
      if (slidings.length > maxLength) {
        val newSliding: Seq[Seq[A]] = sortedIn.sliding(slots + 1, slots + 1).toList
        val slidingSizes = newSliding.map(_.size)
        val slidingDiff = slidingSizes.max - slidingSizes.min
        if (newSliding.length != maxLength || slidingDiff > 1) {
          val right: Seq[Seq[A]] = slidings.takeRight(slidings.length - maxLength)
          val last = slidings(maxLength - 1) ++ right.flatten
          val result: Seq[Seq[A]] = slidings.updated(maxLength - 1, last)
          result.take(maxLength)
        } else {
          newSliding
        }
      } else {
        slidings
      }
    }

  }

  private def fileFrom(fileName: String): InputStream = {
    getClass.getResourceAsStream(fileName)
  }

  def formatedDateBySecs(date: Long): String = {
    formatedDateByMillis(1000L * date)
  }

  private def formatedDateByMillis(date: Long): String = {
    formatedDate(new Date(date))
  }

  private def formatedDate(date: Date): String = {
    new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss").format(date)
  }
}
