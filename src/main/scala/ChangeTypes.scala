import java.util.Date

object ChangeTypes {
	case class VisibleChange(author:Contributor, contributors:Seq[Contributor],
    commitTime:Int, repoName:String) {

    val members = contributors :+ author

    private def authorIsContributor(author:Contributor)(all:Seq[Contributor]):Boolean = {
      val review = all.filter(_.typ.startsWith("Code-Review")).map(_.email)
      return review.contains(author.email)
    }

    def color = changeStatus match {
      case VisibleChangeStatus.warn => "warn"
      case VisibleChangeStatus.ok => "ok"
      case _ => "warn"
    }

    private def formatDate(date:Int) = ReportGenerator.formatedDate(new Date(date * 1000L))

    val title = """|
      |Time: %s
      |Repo: %s
      |""".stripMargin.trim.format(formatDate(commitTime), repoName)

    def changeStatus = members match {
      case c if c.size == 1 => VisibleChangeStatus.warn
      case c if authorIsContributor(author)(c) => VisibleChangeStatus.warn
      case c if c.size >= 2 => VisibleChangeStatus.ok
      case _ => VisibleChangeStatus.warn
    }
  }


  case class VisibleChangeStatus(key:String)

  object VisibleChangeStatus {
    val warn = VisibleChangeStatus("warning")
    val ok = VisibleChangeStatus("ok")
  }

  case class Contributor(email:String, typ:String) {
    val hash = RepoAnalyzer.md5(email)
    val isAuthor = typ == "author"
  }

  case class VisibleRepo(repoName:String, changes:Seq[VisibleChange]) {

    val allChangesCount:Int = changes.size

    val okChangesCount:Int = changes.filter(_.changeStatus == VisibleChangeStatus.ok).size

    def percentageOk():Int = {
      val result:Double = okChangesCount.toDouble / allChangesCount.toDouble * 100
      result.toInt
    }

  }
}
