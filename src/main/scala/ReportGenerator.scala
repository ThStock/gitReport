import com.gilt.handlebars.scala.binding.dynamic._
import com.gilt.handlebars.scala.Handlebars
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.JavaConverters._
import ChangeTypes._

class ReportGenerator(changes:Seq[VisibleChange]) {

  def write(displayLimit:Int) {
    val content = changes
      .take(displayLimit)

    writeByName("truckMap", content)

    val truckByProject = content.groupBy(_.repoName).toSeq
      .map(in => VisibleRepo(in._1, in._2))
      .sortBy(_.repoName).sortWith(_.percentageOk > _.percentageOk)

    writeByName("truckByProject", truckByProject)

  }

  private def writeByName(reportFileName:String, content:Any) {

    val text = scala.io.Source.fromFile("src/main/resources/" + reportFileName + ".mu").mkString
    val template = Handlebars(text)

    val outputDir = new File("out")
    if (!outputDir.isDirectory) {
      outputDir.mkdir()
    }

    val contentMap:Map[String, Any] = Map( //
      "content" -> content,
      "reportDate" -> ReportGenerator.formatedDate(new Date())
    )

    RepoAnalyzer.writeToFile(template(contentMap), new File(outputDir, reportFileName + ".html"))
  }

}

object ReportGenerator {
  def formatedDate(date:Date):String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date)
  }
}
