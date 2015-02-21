import ChangeTypes.Contributor.Activity
import ChangeTypes.{Contributor, VisibleChange, VisibleRepo}
import org.scalatest.{FeatureSpec, GivenWhenThen}

class VisibleRepoSpec extends FeatureSpec with GivenWhenThen {

  def typCopy(c: Contributor, _activity: Activity) = c.copy(activity = _activity, typ = "player")

  def change(c: Contributor, others: Seq[Contributor] = Nil) = VisibleChange(c, others.map(_.copy(typ = "check")), 0, "any")

  val c0q = Contributor("q@example.org", "any")
  val c1a = Contributor("a@example.org", "any")
  val c2c = Contributor("c@example.org", "any")

  scenario("Nil changes") {

    Given("Nil")
    val changes: Seq[VisibleChange] = Nil

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Nil)(result)
  }

  scenario("1 change") {

    Given("1")
    val changes: Seq[VisibleChange] = Seq(change(c0q))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c0q, Activity.MID)))(result)
  }

  scenario("1 change with 2 players") {

    Given("1")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.HIGH), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("2 changes 1 player") {

    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q), change(c0q))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c0q, Activity.MID)))(result)
  }

  scenario("2 changes 2 player with 0, 1 interaction") {

    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q), change(c1a))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c0q, Activity.LOWEST)))(result)
  }

  scenario("2 changes 2 player with 0<-1, 1 interaction") {

    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("2 changes 2 player with 0<-1, 1<-0 interaction") {

    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.HIGHEST), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("3 changes 3 player with 0<-1, 1, 2 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c2c))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c2c, Activity.LOWEST), typCopy(c0q, Activity.HIGH)))(result)
  }

  scenario("3 changes 3 player with 0<-1, 1, 2<-1 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c2c, Seq(c1a)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c2c, Activity.HIGH), typCopy(c0q, Activity.HIGH)))(result)
  }

  scenario("3 changes 2 player with 0<-1, 1, 1<-0 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c1a, Seq(c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("3 changes 3 player with 0<-1, 1<-2, 1<-0 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c2c)), change(c1a, Seq(c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.HIGHEST),typCopy(c2c, Activity.HIGH), typCopy(c0q, Activity.HIGH)))(result)
  }

  scenario("2 changes 2 player with 0<-2, 1<-2 interaction") {
    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c2c)), change(c1a, Seq(c2c)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.HIGH),typCopy(c2c, Activity.HIGH), typCopy(c0q, Activity.HIGH)))(result)
  }

  scenario("2 changes 3 player with 0<-(2,1), 1<-(2,0) interaction") {
    Given("2")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c2c, c1a)), change(c1a, Seq(c2c,c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.HIGHEST),typCopy(c2c, Activity.HIGH), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("3 changes 2 player with 0<-1, 1<-1, 1<-0 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c1a)), change(c1a, Seq(c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.MID), typCopy(c0q, Activity.HIGHEST)))(result)
  }

  scenario("3 changes 2 player with 0, 1, 1 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q), change(c1a), change(c1a))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c0q, Activity.LOWEST)))(result)
  }

  scenario("3 changes 2 player with 0<-0, 1<-1, 1<-1 interaction") {
    Given("3")
    val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c0q)), change(c1a, Seq(c1a)), change(c1a, Seq(c1a)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.LOWEST), typCopy(c0q, Activity.LOWEST)))(result)
  }

  scenario("5 changes 2 player with 3*(0<-1), 1<-1, 1<-0 interaction") {
    Given("5")
    val changes: Seq[VisibleChange] = Seq.fill(3)(change(c0q, Seq(c1a))) ++ Seq(change(c1a, Seq(c1a)), change(c1a, Seq(c0q)))

    When("convert")
    val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

    Then("check")
    assertResult(Seq(typCopy(c1a, Activity.MID), typCopy(c0q, Activity.HIGHEST)))(result)
  }
}
