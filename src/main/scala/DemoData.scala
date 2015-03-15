import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ChangeTypes.{Contributor, VisibleChange, VisibleRepo}

object DemoData {

  def get(): Seq[VisibleRepo] = {

    def chA = vChange("A")(_)
    def chB = vChange("B")(_)
    def chE = vChange("E")(_)

    Seq.tabulate(10)(i => vRepo("r_" + (i + 1), Seq(chE, chA, chB)))
  }

  private def vRepo(name: String, _changes: Seq[(String) => VisibleChange]) = {
    VisibleRepo(repoName = name,
      changes = _changes.map(_.apply(name)),
      branchNames = Seq("a"),
      _repoActivityLimitInDays = 10,
      _activity = 10)
  }

  private def vChange(emailPrefix: String)(repoName: String) = {
    val now = (new Date().getTime / 1000L).toInt + counter.incrementAndGet() * 1000
    val c1 = Contributor(emailPrefix + "@example.org", "author")
    VisibleChange(c1, Nil, now, repoName)
  }

  val counter = new AtomicInteger(1)

}
