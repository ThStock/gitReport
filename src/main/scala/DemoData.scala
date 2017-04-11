import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ChangeTypes._

object DemoData {

  private val counter = new AtomicInteger(1)

  private val r = Some(ChangeType(ContributorType.REVIEWER, Some("z@example.org")))
  private val a = Some(ChangeType(ContributorType.REVIEWER))
  private val n = None

  private case class ChangeType(contributorType: ContributorType, emailChange: Option[String] = None)

  def get(sprintLengthInDays: Int): Seq[VisibleRepo] = {

    def chA = vChange("A")(_)
    def chB = vChange("B")(_)
    def chC = vChange("C")(_)
    def chD = vChange("D")(_)
    def chE = vChange("E")(_)


    val changes = Seq(
      Seq(chD(a), chB(a), chE(r), chA(r), chB(r), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq(chD(a), chB(a), chE(a), chA(r), chB(r), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq.tabulate(25)(i => vChange("D" + i)(r)(_)),
      Seq.tabulate(30)(i => vChange("D" + i % 3)(if (i % 4 == 0) {
        r
      } else {
        a
      })(_)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(a), chB(a), chC(a)),
      Seq(chD(n), chB(n), chE(n), chA(n), chB(n), chB(n), chB(n), chB(n), chD(n), chE(n)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(a), chB(a), chC(a), chB(a), chD(a), chE(r)),
      Seq(chD(a)),Seq(chD(a)),Seq(chD(a)),Seq(chD(a)),Seq(chD(a)),
      Seq(chD(a), chB(a), chE(r), chA(a), chB(r), chB(a), chB(r), chC(a), chB(a), chD(a), chE(r), chE(r), chE(r)),
      Seq.tabulate(27)(i ⇒ vChange("D" + i % 3)(if (i % 100 >= 7) {
        r
      } else {
        a
      })(_))
    )

    val anonymChanges = changes(5).map(_.apply("demo-repo-hidden"))
      .map(in => in.copy(author = in.author.copy(email = "none@example.org")))
      .map(in => in.copy(contributors = in.contributors.map(contrib => contrib.copy(email = "any@example.org"))))

    val manuals = Seq(VisibleRepo(repoName = "demo-repo-hidden", repoFullPath = "/home/any/git/demo-repo-hidden",
      _changes = anonymChanges,
      branchNames = Seq("master"),
      _badges = Nil,
      topCommitter = true,
      _sprintLengthInDays = sprintLengthInDays, participationPercentages = upAndDown()
    ))

    Seq.tabulate(changes.size)(i => vRepo("demo-repo-", i + 1, changes(i), sprintLengthInDays)) ++ manuals
  }

  private def bySprintLenght(sprintLenghtInDays: Int): (ChangeTypes.VisibleChange) => Boolean = _ => true

  private def upAndDown() = normal(in => if (in % 2 == 0) { 50 } else { 100 })

  private def normal(f:(Int => Int)) = Seq.tabulate(19)(f).reverse

  private def vRepo(name: String, number: Int, _changes: Seq[(String) => VisibleChange], sprintLengthInDays: Int) = {
    VisibleRepo(repoName = name + number, repoFullPath = "/home/any/git/" + name + number,
      _changes = _changes.map(_.apply(name + number)).filter(bySprintLenght(sprintLengthInDays)),
      branchNames = Seq.tabulate(number % 3 + 1)(_ + "b"),
      _badges = Seq(VisBadge.moreReviews80(3, 88)),
      _sprintLengthInDays = sprintLengthInDays, participationPercentages = normal(i => i * 10)
    )
  }

  private def vChange(emailPrefix: String)(changeType: Option[ChangeType])(repoName: String) = {
    val next = counter.incrementAndGet()
    val nowSec = (new Date().getTime / 1000L).toInt
    val now = nowSec - next * 60 * 60 * 4
    val c1 = Contributor(emailPrefix + "@example.org", ContributorType.AUTHOR)

    val reviewer = if (changeType.isDefined) {
      Seq(c1.copy(_typ = changeType.get.contributorType,
        email = changeType.get.emailChange.getOrElse(c1.email)))
    } else {
      Nil
    }


    VisibleChange(c1, reviewer, now, repoName, "/home/any/git/" + repoName, true)
  }

}
