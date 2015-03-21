import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ChangeTypes.{Contributor, ContributorType, VisibleChange, VisibleRepo}

object DemoData {

  def get(): Seq[VisibleRepo] = {

    def chA = vChange("A")(_)
    def chB = vChange("B")(_)
    def chC = vChange("C")(_)
    def chD = vChange("D")(_)
    def chE = vChange("E")(_)


    val changes = Seq(
      Seq(chD(a), chB(a), chE(r), chA(r), chB(r), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq(chD(a), chB(a), chE(a), chA(r), chB(r), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq.tabulate(20)(i => vChange("D" + i)(r)(_)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(r), chB(r), chC(r), chB(r), chD(r), chE(r)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(a), chB(a), chC(a), chB(a), chD(a), chE(a)),
      Seq(chD(n), chB(n), chE(n), chA(n), chB(n), chB(n), chB(n), chB(n), chD(n), chE(n)),
      Seq(chD(a), chB(a), chE(a), chA(a), chB(a), chB(a), chB(a), chC(a), chB(a), chD(a), chE(r))
    )
    Seq.tabulate(7)(i => vRepo("demo-repo-", i + 1, changes(i)))
  }

  private def vRepo(name: String, number: Int, _changes: Seq[(String) => VisibleChange]) = {
    VisibleRepo(repoName = name + number,
      _changes = _changes.map(_.apply(name + number)),
      branchNames = Seq.tabulate(number % 3 + 1)(_ + "b"),
      _sprintLengthInDays = 14,
      _activity = 10)
  }

  private def vChange(emailPrefix: String)(changeType: Option[ChangeType])(repoName: String) = {
    val next = counter.incrementAndGet()
    val now = (new Date().getTime / 1000L).toInt + next * 1000
    val c1 = Contributor(emailPrefix + "@example.org", Contributor.AUTHOR)

    val reviewer = if (changeType.isDefined) {
      Seq(c1.copy(_typ = changeType.get.contributorType,
        email = changeType.get.emailChange.getOrElse(c1.email)))
    } else {
      Nil
    }


    VisibleChange(c1, reviewer, now, repoName)
  }

  val counter = new AtomicInteger(1)

  val r = Some(ChangeType(Contributor.REVIWER, Some("z@example.org")))
  val a = Some(ChangeType(Contributor.REVIWER))
  val n = None

  case class ChangeType(contributorType: ContributorType, emailChange: Option[String] = None)

}
