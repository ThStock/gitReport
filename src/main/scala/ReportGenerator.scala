import java.io.{File, InputStream}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import ChangeTypes._
import ReportGenerator._
import com.github.jknack.handlebars.{Handlebars, Helper, Options}

import scala.beans.BeanProperty
import scala.collection.JavaConversions

class ReportGenerator(repos: Seq[VisibleRepoT]) {

  def write(sprintLengthInDays: Int, displayLimit: Int, repoActivityLimit: Int) {
    if (repos != Nil) {
      val content: Seq[VisibleChangeT] = repos.flatMap(_.changes).sortBy(_.commitTimeMillis).reverse

      writeTruckByRepo(repoActivityLimit, content, displayLimit, sprintLengthInDays, new DiskIo(outDir), new Date())
    }
  }

  private lazy val repoFullPathToRepos: Map[String, VisibleRepoT] = repos.groupBy(_.repoFullPath)
    .map(in ⇒ (in._1, in._2.head))

  def writeTruckByRepo(repoActivityLimit: Int,
                       content: Seq[VisibleChangeT],
                       displayLimit: Int,
                       sprintLengthInDays: Int,
                       diskIo: DiskIoT, now: Date) {
    val fullPathToBranchNames: Map[String, Seq[String]] = repoFullPathToRepos
      .map(kv => (kv._1, kv._2.branchNames))

    def branchNamesOf(key: String): Seq[String] = {

      val branches: Seq[String] = fullPathToBranchNames.getOrElse(key, Nil)
      if (branches.nonEmpty) {
        branches
      } else {
        throw new IllegalStateException("no branches for " + key + " in " + fullPathToBranchNames)
      }
    }

    val latestCommitDate = if (content.isEmpty) {
      0
    } else {
      content.map(_.commitTimeMillis).max
    }

    def writeReport(now: Date) {

      val filterCommitDate = latestCommitDate
      val contentListed = content.filter(_.commitTimeMillis <= filterCommitDate).take(displayLimit)
      if (contentListed != Nil) {
        val contentGrouped = contentListed.groupBy(_.repoFullPath.toString) // TODO group by repo full name
          .filter(_._2.size > repoActivityLimit)

        val truckByProject: Seq[VisibleRepo] = contentGrouped.toSeq.map { in =>
          val repoFullPath = in._2.map(_.repoFullPath).head.toString
          val repoName = in._2.map(_.repoName).head.toString
          val rOpt = repoFullPathToRepos.get(repoFullPath)
          val r = if (rOpt.isDefined) {
            rOpt.get
          } else {
            val repoNames = repoFullPathToRepos.map(_._1)
            throw new IllegalStateException("no repo for fullPath: " + repoFullPath + " in " + repoNames)
          }
          VisibleRepo(repoName = repoName,
            repoFullPath = repoFullPath,
            _changes = in._2,
            _badges = r.badges,
            branchNames = branchNamesOf(repoFullPath),
            _sprintLengthInDays = sprintLengthInDays,
            participationPercentages = r.participationPercentages,
            _activity = ReportGenerator.repoActivityScoreOf(in._1, contentGrouped).intValue
          )
        }

        if (truckByProject == Nil) {
          println("W: no repos will appear in report")
        }

        val topCommitts = if (truckByProject != Nil) {
          truckByProject.filter(_._activity > 1).map(_.mainCommitters).max
        } else {
          0
        }
        val markedTopComitter = truckByProject.map(r => if (r.mainCommitters == topCommitts && r._activity > 1) {
          r.copy(topCommitter = true)
        } else {
          r
        })

        val segments = ReportGenerator.slidingsOf3 {
          markedTopComitter
            .sortBy(_.repoName)
            .sortBy(_.allChangesCount)
            .reverse
            .sortWith(_.percentageOk > _.percentageOk)
        }

        val segemnts = Segmented(slots = Seq(Slot(segments._1), Slot(segments._2), Slot(segments._3)),
          latestCommitDate = ReportGenerator.formatedDateByMillis(contentListed.map(_.commitTimeMillis).min),
          newestCommitDate = ReportGenerator.formatedDateByMillis(filterCommitDate),
          sprintLength = sprintLengthInDays)
        diskIo.writeByNameToDisk("truckByProject", segemnts, now, "index")

      }
    }

    writeReport(now)
    diskIo.copyToOutputFolder("octoicons/octicons.css")
    diskIo.copyToOutputFolder("octoicons/octicons.eot")
    diskIo.copyToOutputFolder("octoicons/octicons.svg")
    diskIo.copyToOutputFolder("octoicons/octicons.woff")
    diskIo.copyToOutputFolder("bootstrap-3.3.6-dist/css/bootstrap.min.css")
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

  case class Slot(@BeanProperty repos: Seq[VisibleRepo])

  case class Segmented(@BeanProperty slots: Seq[Slot], @BeanProperty newestCommitDate: String,
                       @BeanProperty latestCommitDate: String, @BeanProperty sprintLength: Int)

  trait DiskIoT {
    def copyToOutputFolder(path: String)

    def writeByNameToDisk(reportFileName: String, content: Segmented, now: Date, outputFileName: String = "")
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

    def writeByNameToDisk(reportFileName: String, content: Segmented, now: Date, outputFileName: String = "") {
      val fileName = reportFileName + ".mu" // TODO hbs
      val text = io.Source.fromInputStream(getClass.getResourceAsStream(fileName), "utf-8").mkString

      val contentMap: Map[String, Any] = Map("content" → content, "reportDate" → ReportGenerator.formatedDate(now))
      val outFileNameWithSuffix = if (outputFileName.isEmpty) {
        reportFileName
      } else {
        outputFileName
      }
      val outputFile = new File(outDir, outFileNameWithSuffix + ".html")

      RepoAnalyzer.writeToFile(render(text, contentMap), outputFile)
      println("written: " + outputFile.getAbsolutePath)
    }
  }

  def render(templateRaw: String, contentMap: Map[String, Any]): String = {
    val hbs = new Handlebars()
    hbs.registerHelperMissing(new Helper[Any]() {
      override def apply(context: Any, options: Options) =
        throw new IllegalStateException(options.fn.text())
    })
    val template = hbs.compileInline(templateRaw)
    template(toJavaTypes(contentMap))
  }

  private def getCCParams(cc: AnyRef): Map[String, Any] =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName → f.get(cc))
    }.filterKeys(key ⇒ key != "$outer")

  private def getCCMethods(cc: AnyRef): Map[String, Any] =
    (Map[String, Any]() /: cc.getClass.getDeclaredMethods.toSeq.filter(_.getParameterCount == 0)) { (a, f) =>
      f.setAccessible(true)
      a + (f.getName → f.invoke(cc))
    }

  private def isCaseClass(o: AnyRef) = o.getClass.getInterfaces.contains(classOf[scala.Product])

  private def toJavaTypes(x: Any): Any = x match {
    case e: Map[_, _] ⇒ JavaConversions.mapAsJavaMap(e.mapValues(toJavaTypes))
    case e: Seq[_] ⇒ JavaConversions.asJavaCollection(e.map(toJavaTypes))
    case e: AnyRef if isCaseClass(e) ⇒ toJavaTypes(getCCParams(e) ++ getCCMethods(e))
    case _ => x;
  }

  def slidingsOf3[A](sortedIn: Seq[A]): (Seq[A], Seq[A], Seq[A]) = {
    val result = slidingsOf(3)(sortedIn)
    (result(0), result(1), result(2))
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

  def formatedDateByMillis(date: Long): String = {
    formatedDate(new Date(date))
  }

  def formatedDate(date: Date): String = {
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
