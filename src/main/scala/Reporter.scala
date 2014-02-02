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
    commitTime:Int)

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

  def formatTeam(team:VisibleChange):String = {

    val members = (team.author +: team.contributors)
    def authorIsContributor(author:Contributor)(all:Seq[Contributor]):Boolean = {
      val review = all.filter(_.typ.startsWith("Code-Review")).map(_.email)
      return review.contains(author.email)
    }
    val color = members match {
      case c if c.size == 1 => "warn"
      case c if authorIsContributor(team.author)(c) => "warn"
      case c if c.size >= 2 => "ok"
      case _ => "warn"
    }
    import java.text.SimpleDateFormat
    import java.util.Date
    val title = "Time: " + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      .format(new Date(team.commitTime * 1000L))
    return """|
       |<div class="team %s" title="%s">
       |""".stripMargin.trim.format(color, title) +
       members.map(_.icon).mkString("<div class=\"spacer\"></div>") + "</div>"
  }

  // TODO extract to template
  val html = """
  |<html>
  |<head>
  |<style>
  |body {
  |  background-color: #F5F5F5;
  |}
  |.contributor {
  |  border-radius:3px;
  |  border: 0px solid gray;
  |  opacity: .3;
  |  -webkit-filter: grayscale(1);
  |  height: 40px;
  |  width: 40px;
  |  margin: 0;
  |  padding: 0;
  |  float: left;
  |}
  |.team {
  |  border-radius:3px;
  |  border: 1px solid gray;
  |  margin: 2px;
  |  float: left;
  |  height: 55px;
  |}
  |.team .spacer {
  |  width: 6px;
  |  height: 10px;
  |  margin: 0;
  |  float: left;
  |}
  |.team.warn {
  |  background-color: red;
  |}
  |.team.ok {
  |  background-color: green;
  |}
  |</style>
  |</head>
  |<body style="text-align: center;">
  |<h5>Gerrit Truck Trend Report</h5>
  |%s
  |</body>
  |</html>
  """.stripMargin.format(changes
        .sortWith(_.commitTime > _.commitTime)
        .map(formatTeam)
        .take(displayLimit).mkString(" "))

  writeToFile(html, new File("index.html"))

  case class Contributor(email:String, typ:String) {
    def icon:String = {
      val hash = md5(email)
      return """
        |<img src="https://%s.gravatar.com/avatar/%s?s=80&d=identicon"
        | title="%s"
        | class="contributor" >
        |""".stripMargin.trim.format("lb", hash, email + " " + typ)
    }
  }
}
