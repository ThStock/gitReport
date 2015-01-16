import java.util.Date

object ChangeTypes {
	case class VisibleChange(author:Contributor, contributors:Seq[Contributor],
    commitTime:Int, repoName:String) {

    val members = contributors :+ author

    private def authorIsContributor(author:Contributor)(all:Seq[Contributor]):Boolean = {
      val review = all.filter(_.typ.startsWith("Code-Review")).map(_.email)
      return review.contains(author.email)
    }

    val color = members match {
      case c if c.size == 1 => "warn"
      case c if authorIsContributor(author)(c) => "warn"
      case c if c.size >= 2 => "ok"
      case _ => "warn"
    }
    def formatDate(date:Int) = ReportGenerator.formatedDate(new Date(date * 1000L))

    val title = """|
      |Time: %s
      |Repo: %s
      |""".stripMargin.trim.format(formatDate(commitTime), repoName)
  }
   case class Contributor(email:String, typ:String) {
    val hash = RepoAnalyzer.md5(email)
    val isAuthor = typ == "author"
  }
}
