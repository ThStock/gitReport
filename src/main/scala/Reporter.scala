import java.io._
import RepoAnalyzer._
import org.fusesource.scalate.TemplateEngine
import java.text.SimpleDateFormat
import java.util.Date


object Reporter extends App {
  /*
   * git config --add remote.origin.fetch +refs/notes/review:refs/notes/review
   * git config notes.displayRef refs/notes/review
   * git fetch
   */

  def argsOpt(arrayPosition:Int):Option[String] = arrayPosition match {
      case x if args.length > x =>  Some(args(x))
      case _                     => None
    }

  val displayLimit = argsOpt(0).getOrElse("700").toInt
  val repos = new File(argsOpt(1).getOrElse("../"))
  val commitLimit = argsOpt(2).getOrElse("1200").toInt

  def findRecursiv(file:File, filter:File => Boolean):Seq[File] = {
    val files = file.listFiles
    return files.filter(filter) match {
      case notFound if notFound.isEmpty => {
        files.filter(_.isDirectory)
          .flatMap(findRecursiv(_,filter))
      }
      case found => found
    }
  }
  def isGitDir(f:File):Boolean = f.isDirectory && f.getName == ".git"
  val repoDirs:Seq[File] = findRecursiv(repos, isGitDir)

  private def formatedDate(date:Date):String = {
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date)
  }

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
    def formatDate(date:Int) = formatedDate(new Date(date * 1000L))

    val title = """|
      |Time: %s
      |Repo: %s
      |""".stripMargin.trim.format(formatDate(commitTime), repoName)
  }

  val changes:Seq[VisibleChange] = repoDirs.sorted.map{ repo =>
    println(repo)
    val analy = new RepoAnalyzer(repo, commitLimit)
    val allChanges:Seq[Change] = analy.getChanges()
    def toVisChange(repoName:String)(change:Change):VisibleChange = {
      val author = Contributor(change.authorEmail, "author")
      // TODO handle SignedOfBy
      val reviewers:Seq[FooterElement] = change.footer
        .filter(_.key.startsWith("Code-Review"))

      val contribs:Seq[Contributor] = reviewers
        .map(foot => Contributor(foot.email.getOrElse(foot.value), foot.key))
      return VisibleChange(author, contribs, change.commitTime, repoName)
    }

    val result:Seq[VisibleChange] = allChanges.map(toVisChange(analy.name()))
    result
  }.flatten


  val text = scala.io.Source.fromFile("src/main/resources/truckMap.mu").mkString
  val content = changes.sortWith(_.commitTime > _.commitTime)
    .take(displayLimit)

  val engine = new TemplateEngine
  val template = engine.compileMoustache(text)

  val outputDir = new File("out")
  if (!outputDir.isDirectory) {
    outputDir.mkdir()
  }
  writeToFile(engine.layout("",template, Map("content" -> content, "reportDate" -> formatedDate(new Date()))),
    new File(outputDir, "truckMap.html"))

  case class Contributor(email:String, typ:String) {
    val hash = md5(email)
    val isAuthor = typ == "author"
  }
}
