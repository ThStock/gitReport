import java.io.{File, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import java.text.SimpleDateFormat
import java.util.Date

import ChangeTypes._
import com.gilt.handlebars.scala.Handlebars
import com.gilt.handlebars.scala.binding.dynamic._

class ReportGenerator(repos: Seq[VisibleRepo]) {

  def write(sprintLengthInDays: Int, displayLimit: Int, repoActivityLimit: Int) {
    if (repos != Nil) {
      val content: Seq[VisibleChange] = repos.flatMap(_.changes)
        .sortBy(_.commitTime).reverse

      writeTruckByRepo(repoActivityLimit, content, displayLimit, sprintLengthInDays)
    }
  }

  private def writeTruckByRepo(repoActivityLimit: Int, content: Seq[VisibleChange], //
                               displayLimit: Int, sprintLengthInDays: Int) {
    val repoByName = repos.groupBy(_.repoName)
    def branchNamesOf(key: String) = repoByName.get(key).get.head.branchNames

    val latestCommitDate = content.map(_.commitTime).max.toLong

    def write(dayDelta: Int) {

      val filterCommitDate = latestCommitDate - dayDelta * 86400L
      val contentListed = content
        .filter(_.commitTime <= filterCommitDate)
        .take(displayLimit)
      if (contentListed != Nil) {
        val contentGrouped = contentListed
          .groupBy(_.repoName)

        val truckByProject: Seq[VisibleRepo] = contentGrouped.toSeq
          .map(in => VisibleRepo(in._1, in._2, branchNamesOf(in._1), sprintLengthInDays, scoreOf(in._1, repoActivityLimit, contentGrouped)))
          .filter(_.changes.size > repoActivityLimit)

        if (truckByProject == Nil) {
          println("W: no repos will appear in report")
        }

        val topCommitts = if (truckByProject != Nil) {
          truckByProject.filter(_._activity > 1).map(_.mainComitters).max
        } else {
          0
        }
        val markedTopComitter = truckByProject
          .map(r => if (r.mainComitters == topCommitts && r._activity > 1) {
          r.copy(topComitter = true)
        } else {
          r
        })

        case class Slot(repos: Seq[VisibleRepo])

        case class Segmented(slots: Seq[Slot], newestCommitDate: String, latestCommitDate: String, sprintLength:Int)
        val segments = ReportGenerator.slidingsOf(3)(markedTopComitter
          .sortBy(_.repoName).sortBy(_.allChangesCount).reverse.sortWith(_.percentageOk > _.percentageOk)
        )

        val segemnts = Segmented(slots = Seq(Slot(segments(0)), Slot(segments(1)), Slot(segments(2))),
          latestCommitDate = ReportGenerator.formatedDateBySecs(contentListed.map(_.commitTime).min),
          newestCommitDate = ReportGenerator.formatedDateBySecs(filterCommitDate),
          sprintLength = sprintLengthInDays
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

    val text = io.Source.fromInputStream(getClass.getResourceAsStream(fileName), "utf-8").mkString
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
    def slide(_maxLength: Int, _fillMod: Int, _fillPerSlot: Int, _sortedIn: Seq[A]): Seq[Seq[A]] = {
      val mod = if (_fillMod > 0) {
        _fillPerSlot + 1
      } else {
        _fillPerSlot
      }
      if (_sortedIn == Nil) {
        Nil
      } else {
        Seq(_sortedIn.take(mod)) ++ slide(_maxLength, _fillMod - 1, _fillPerSlot, _sortedIn.drop(mod))
      }
    }

    val result = slide(maxLength, sortedIn.size % maxLength, sortedIn.size / maxLength, sortedIn)
    if (result.size < maxLength) {
      result ++ Seq.fill(maxLength - result.size)(Nil)
    } else {
      result
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
