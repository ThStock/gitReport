import org.fusesource.scalate.TemplateEngine
import java.io.File
import java.util.Date
import java.text.SimpleDateFormat
import scala.collection.JavaConversions._
import ChangeTypes._

class ReportGenerator(changes:Seq[VisibleChange]) {

  def write(displayLimit:Int) {
    val content = changes
      .sortWith(_.commitTime > _.commitTime)
      .take(displayLimit)

    writeByName("truckMap", content)

  }

  private def writeByName(reportFileName:String, content:Seq[ChangeTypes.VisibleChange]) {
    val engine = new TemplateEngine
    val text = scala.io.Source.fromFile("src/main/resources/" + reportFileName + ".mu").mkString
    val template = engine.compileMoustache(text)

    val outputDir = new File("out")
    if (!outputDir.isDirectory) {
      outputDir.mkdir()
    }

    val contentMap:Map[String, Any] = Map( //
      "truckMapContent" -> content,
      "reportDate" -> ReportGenerator.formatedDate(new Date())
    )

    RepoAnalyzer.writeToFile(engine.layout("", template, contentMap), new File(outputDir, reportFileName + ".html"))
  }

}

object ReportGenerator {
  def formatedDate(date:Date):String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date)
  }

}
