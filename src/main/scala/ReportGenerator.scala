import java.io.{File, InputStream}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import ChangeTypes._
import ReportGenerator.{DiskIo, DiskIoT, Segmented, Slot}
import com.gilt.handlebars.scala.Handlebars
import com.gilt.handlebars.scala.binding.dynamic._

class ReportGenerator(repos: Seq[VisibleRepoT]) {

  def write(sprintLengthInDays: Int, displayLimit: Int, repoActivityLimit: Int) {
    if (repos != Nil) {
      val content: Seq[VisibleChangeT] = repos.flatMap(_.changes).sortBy(_.commitTime).reverse

      writeTruckByRepo(repoActivityLimit, content, displayLimit, sprintLengthInDays, new DiskIo(outDir))
    }
  }

  def writeTruckByRepo(repoActivityLimit: Int,
                       content: Seq[VisibleChangeT],
                       displayLimit: Int,
                       sprintLengthInDays: Int,
                       diskIo: DiskIoT) {
    val fullPathToBranchNames:Map[String, Seq[String]] = repos.groupBy(_.repoFullPath)
      .map(kv => (kv._1, kv._2.flatMap(_.branchNames)))
    def branchNamesOf(key: String):Seq[String] = {

      val branches:Seq[String] = fullPathToBranchNames.getOrElse(key, Nil)
      if (branches.nonEmpty) {
        branches
      } else {
        throw new IllegalStateException("no branches for " + key + " in " + fullPathToBranchNames)
      }
    }

    val latestCommitDate = if (content.isEmpty) {
      0
    } else {
      content.map(_.commitTime).max.toLong
    }

    def writeReport(dayDelta: Int) {

      val filterCommitDate = latestCommitDate - dayDelta * 86400L
      val contentListed = content.filter(_.commitTime <= filterCommitDate).take(displayLimit)
      if (contentListed != Nil) {
        val contentGrouped = contentListed.groupBy(_.repoFullPath.toString) // TODO group by repo full name
          .filter(_._2.size > repoActivityLimit)

        val truckByProject: Seq[VisibleRepo] = contentGrouped.toSeq.map { in =>
          val repoFullPath = in._2.map(_.repoFullPath).head.toString
          val repoName = in._2.map(_.repoName).head.toString
          VisibleRepo(repoName,
                       repoFullPath,
                       in._2,
                       branchNamesOf(repoFullPath),
                       sprintLengthInDays,
                       ReportGenerator.repoActivityScoreOf(in._1, contentGrouped).intValue)
        }

        if (truckByProject == Nil) {
          println("W: no repos will appear in report")
        }

        val topCommitts = if (truckByProject != Nil) {
          truckByProject.filter(_._activity > 1).map(_.mainComitters).max
        } else {
          0
        }
        val markedTopComitter = truckByProject.map(r => if (r.mainComitters == topCommitts && r._activity > 1) {
          r.copy(topComitter = true)
        } else {
          r
        })

        val segments = ReportGenerator.slidingsOf(3) {
          markedTopComitter.sortBy(_.repoName).sortBy(_.allChangesCount).reverse.sortWith(_.percentageOk > _.percentageOk)
        }

        val segemnts = Segmented(slots = Seq(Slot(segments(0)), Slot(segments(1)), Slot(segments(2))),
                                  latestCommitDate = ReportGenerator.formatedDateBySecs(contentListed.map(_.commitTime).min),
                                  newestCommitDate = ReportGenerator.formatedDateBySecs(filterCommitDate),
                                  sprintLength = sprintLengthInDays)
        diskIo.writeByNameToDisk("truckByProject", segemnts, "truckByProject" + dayDelta)

      }
    }

    writeReport(0)
    diskIo.copyToOutputFolder("octoicons/octicons.css")
    diskIo.copyToOutputFolder("octoicons/octicons.eot")
    diskIo.copyToOutputFolder("octoicons/octicons.svg")
    diskIo.copyToOutputFolder("octoicons/octicons.woff")
    diskIo.copyToOutputFolder("bootstrap-3.3.2-dist/css/bootstrap.min.css")
    diskIo.copyToOutputFolder("git-report-xs.png")
    diskIo.copyToOutputFolder("git-report.svg")
  }

  private lazy val outDir: File = {
    val out = new File("out")
    if (!out.isDirectory) {
      out.mkdir()
    }
    out
  }

}

object ReportGenerator {

  case class Slot(repos: Seq[VisibleRepo])

  case class Segmented(slots: Seq[Slot], newestCommitDate: String, latestCommitDate: String, sprintLength: Int)

  trait DiskIoT {
    def copyToOutputFolder(path: String)

    def writeByNameToDisk(reportFileName: String, content: Any, outputFileName: String = "")
  }

  class DiskIo(outDir: File) extends DiskIoT {

    def copyToOutputFolder(path: String) {
      val outputFilename = outDir.toPath.resolve(path)
      val parant = outputFilename.getParent
      if (!Files.isDirectory(parant)) {
        Files.createDirectories(parant)
      }
      if (!Files.exists(outputFilename)) {
        Files.copy(ReportGenerator.fileFrom(path), outputFilename)
      }
    }

    def writeByNameToDisk(reportFileName: String, content: Any, outputFileName: String = "") {
      val fileName = reportFileName + ".mu" // TODO hbs
      val text = io.Source.fromInputStream(getClass.getResourceAsStream(fileName), "utf-8").mkString
      val template = Handlebars(text)

      val contentMap: Map[String, Any] = Map("content" → content, "reportDate" → ReportGenerator.formatedDate(new Date()))
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

  case class ActivityScore(value: Int) {
    def intValue = value
  }

  object ActivityScore {
    val high = ActivityScore(2)
    val mid = ActivityScore(1)
    val low = ActivityScore(0)
  }

  def repoActivityScoreOf(repoName: String, contentGrouped: Map[String, Seq[VisibleChangeT]]): ActivityScore = {
    val slides = slidingsOf(3)(contentGrouped.map(_._2.size).filterNot(_ == 0).toSet.toSeq.sorted.reverse)
    val scores = (slides(0).map(in ⇒ (in, ActivityScore.high)) ++
      slides(1).map(in ⇒ (in, ActivityScore.mid)) ++
      slides(2).map(in ⇒ (in, ActivityScore.low))).foldLeft(Map[Int, ActivityScore]())(_ + _)

    val changeCount: Int = contentGrouped.get(repoName).get.size


    scores(changeCount)
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

  def median(in: Seq[Int]): Double = {
    if (in == Nil) {
      0
    } else {
      val (lower, upper) = in.sorted.splitAt(in.size / 2)
      if (in.size % 2 == 0) (lower.last + upper.head) / 2d else upper.head
    }
  }
}
