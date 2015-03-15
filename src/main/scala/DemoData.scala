import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ChangeTypes.{ContributorType, Contributor, VisibleChange, VisibleRepo}

object DemoData {

  def get(): Seq[VisibleRepo] = {

    def chA = vChange("A")(_)
    def chB = vChange("B")(_)
    def chE = vChange("E")(_)

    Seq.tabulate(10)(i => vRepo("git-repo-" + (i + 1), Seq(chE, chA, chB, chB, chB)))
  }

  private def vRepo(name: String, _changes: Seq[(String) => VisibleChange]) = {
    VisibleRepo(repoName = name,
      changes = _changes.map(_.apply(name)),
      branchNames = Seq("a"),
      _repoActivityLimitInDays = 10,
      _activity = 10)
  }

  private def vChange(emailPrefix: String)(repoName: String) = {
    val next = counter.incrementAndGet()
    val now = (new Date().getTime / 1000L).toInt + next * 1000
    val c1 = Contributor(emailPrefix + "@example.org", Contributor.AUTHOR)

    val reviewer = if (next % 5 == 0) {
      Seq(c1.copy(_typ = ContributorType("any")))
    } else {
      Seq(c1.copy(_typ = Contributor.REVIWER))
    }

    VisibleChange(c1, reviewer, now, repoName)
  }

  val counter = new AtomicInteger(1)

}
