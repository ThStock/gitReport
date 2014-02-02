import java.io._

object Reporter extends App {
  import RepoAnalyzer._

  /*
   * git config --add remote.origin.fetch +refs/notes/review:refs/notes/review
   * git config notes.displayRef refs/notes/review
   * git fetch
   */

  def argsOpt(arrayPosition:Int):Option[String] = arrayPosition match {
      case x if args.length > x =>  Some(args(x))
      case _                     => None
    }

  val displayLimit = argsOpt(0).getOrElse("200").toInt
  val repos = new File(argsOpt(1).getOrElse("../"))
  val commitLimit = 200

  val dirs = repos.listFiles.toSeq.filter(f => f.isDirectory)

  val repoDirs:Seq[File] = dirs.map(dir => new File(dir, ".git"))
    .filter(_.isDirectory)

  case class VisibleChange(author:Contributor, contributors:Seq[Contributor],
    commitTime:Int) {
    import java.text.SimpleDateFormat
    import java.util.Date

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

    val title = "Time: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      .format(new Date(commitTime * 1000L))
  }

  val changes:Seq[VisibleChange] = repoDirs.sorted.map{ repo =>
    val analy = new RepoAnalyzer(repo, commitLimit)
    val allChanges:Seq[Change] = analy.getChanges()
    def toVisChange(change:Change):VisibleChange = {
      val author = Contributor(change.authorEmail, "author")
      // TODO handle SignedOfBy
      val reviewers:Seq[FooterElement] = change.footer
        .filter(_.key.startsWith("Code-Review"))

      val contribs:Seq[Contributor] = reviewers
        .map(foot => Contributor(foot.email.getOrElse(foot.value), foot.key))
      return VisibleChange(author, contribs, change.commitTime)
    }

    val result:Seq[VisibleChange] = allChanges.map(toVisChange)
    result
  }.flatten

  val text = scala.io.Source.fromFile("src/main/resources/index.mu").mkString
  val content = changes.sortWith(_.commitTime > _.commitTime)
    .take(displayLimit)

  import org.fusesource.scalate.TemplateEngine
  val engine = new TemplateEngine
  val template = engine.compileMoustache(text)

  writeToFile(engine.layout("",template, Map("content" -> content)), new File("index.html"))

  case class Contributor(email:String, typ:String) {
    val hash = md5(email)
    val isAuthor = typ == "author"
  }
}
