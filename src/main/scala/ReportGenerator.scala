import com.gilt.handlebars.scala.binding.dynamic._
import com.gilt.handlebars.scala.Handlebars
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.JavaConverters._
import ChangeTypes._

class ReportGenerator(repos:Seq[VisibleRepo]) {

  def write(displayLimit:Int, repoActivityLimit:Int) {
    val content:Seq[VisibleChange] = repos.flatMap(_.changes)
      .sortBy(_.commitTime).reverse
      .take(displayLimit)

    writeByName("truckMap", content)

    val repoByName = repos.groupBy(_.repoName)
    def branchNamesOf(key:String) = repoByName.get(key).get.head.branchNames

    val truckByProject:Seq[VisibleRepo] = content.groupBy(_.repoName).toSeq
      .map(in => VisibleRepo(in._1, in._2, branchNamesOf(in._1)))
      .filter(_.changes.size > repoActivityLimit)
      .sortBy(_.repoName).sortWith(_.percentageOk > _.percentageOk)

    writeByName("truckByProject", truckByProject)

  }

  private def writeByName(reportFileName:String, content:Any) {
    val fileName = reportFileName + ".mu"

    val text = io.Source.fromInputStream(getClass.getResourceAsStream(fileName)).mkString
    val template = Handlebars(text)

    val outputDir = new File("out")
    if (!outputDir.isDirectory) {
      outputDir.mkdir()
    }

    val contentMap:Map[String, Any] = Map( //
      "content" -> content,
      "reportDate" -> ReportGenerator.formatedDate(new Date())
    )
    val outputFile = new File(outputDir, reportFileName + ".html")
    RepoAnalyzer.writeToFile(template(contentMap), outputFile)
    println("written: " + outputFile.getAbsolutePath)
  }

}

object ReportGenerator {
  def formatedDate(date:Date):String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date)
  }
}
