import ChangeTypes.Contributor.ContributorActivity
import ChangeTypes.{Contributor, ContributorType, VisibleChange, VisibleRepo}
import org.scalatest.{FeatureSpec, GivenWhenThen}

class VisibleRepoSpec extends FeatureSpec with GivenWhenThen {

  def typCopy(c: Contributor, _activity: ContributorActivity) = c.copy(activity = _activity, _typ = ContributorType("player"))

  def change(author: Contributor, others: Seq[Contributor] = Nil) = //
    VisibleChange(author.copyAsAuthor(), others, 0, "any", "/a/any", true)

  val c0q = Contributor("q@example.org", ContributorType("any"))
  val c1a = Contributor("a@example.org", ContributorType("any"))
  val c2c = Contributor("c@example.org", ContributorType("any"))

  feature("contributors") {
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
      assertResult(Seq(typCopy(c0q, ContributorActivity.MID)))(result)
    }

    scenario("1 change with 2 players") {
      Given("1")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGH), typCopy(c0q, ContributorActivity.MID)))(result)
    }

    scenario("1 change with 2 players both author") {
      Given("1")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a.copyAsAuthor(), c0q.copyAsAuthor())))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGH), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }

    scenario("2 changes 1 player") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q), change(c0q))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c0q, ContributorActivity.MID)))(result)
    }

    scenario("2 changes 2 player with 0, 1 interaction") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q), change(c1a))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST), typCopy(c0q, ContributorActivity.LOWEST)))(result)
    }

    scenario("2 changes 2 player with 0<-1, 1 interaction") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }

    scenario("2 changes 2 player with 0<-1, 1<-0 interaction") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGHEST), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }

    scenario("3 changes 3 player with 0<-1, 1, 2 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c2c))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST),
        typCopy(c2c, ContributorActivity.LOWEST),
        typCopy(c0q, ContributorActivity.HIGHEST)
      )
      )(result)
    }

    scenario("3 changes 3 player with 0<-1, 1, 2<-1 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c2c, Seq(c1a)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST),
        typCopy(c2c, ContributorActivity.HIGHEST),
        typCopy(c0q, ContributorActivity.HIGHEST)
      )
      )(result)
    }

    scenario("3 changes 2 player with 0<-1, 1, 1<-0 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a), change(c1a, Seq(c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }

    scenario("3 changes 3 player with 0<-1, 1<-2, 1<-0 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c2c)), change(c1a, Seq(c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGHEST),
        typCopy(c2c, ContributorActivity.HIGH),
        typCopy(c0q, ContributorActivity.HIGHEST)
      )
      )(result)
    }

    scenario("2 changes 2 player with 0<-2, 1<-2 interaction") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c2c)), change(c1a, Seq(c2c)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGHEST),
        typCopy(c2c, ContributorActivity.HIGH),
        typCopy(c0q, ContributorActivity.HIGHEST)
      )
      )(result)
    }

    scenario("2 changes 3 player with 0<-(2,1), 1<-(2,0) interaction") {
      Given("2")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c2c, c1a)), change(c1a, Seq(c2c, c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.HIGHEST),
        typCopy(c2c, ContributorActivity.HIGH),
        typCopy(c0q, ContributorActivity.HIGHEST)
      )
      )(result)
    }

    scenario("3 changes 2 player with 0<-1, 1<-1, 1<-0 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c1a)), change(c1a, Seq(c1a)), change(c1a, Seq(c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.MID), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }

    scenario("3 changes 2 player with 0, 1, 1 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q), change(c1a), change(c1a))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST), typCopy(c0q, ContributorActivity.LOWEST)))(result)
    }

    scenario("3 changes 2 player with 0<-0, 1<-1, 1<-1 interaction") {
      Given("3")
      val changes: Seq[VisibleChange] = Seq(change(c0q, Seq(c0q)), change(c1a, Seq(c1a)), change(c1a, Seq(c1a)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.LOWEST), typCopy(c0q, ContributorActivity.LOWEST)))(result)
    }

    scenario("5 changes 2 player with 3*(0<-1), 1<-1, 1<-0 interaction") {
      Given("5")
      val changes: Seq[VisibleChange] = Seq.fill(3)(change(c0q, Seq(c1a))) ++ Seq(change(c1a, Seq(c1a)), change(c1a, Seq(c0q)))

      When("convert")
      val result: Seq[Contributor] = VisibleRepo.toContibutors(changes)

      Then("check")
      assertResult(Seq(typCopy(c1a, ContributorActivity.MID), typCopy(c0q, ContributorActivity.HIGHEST)))(result)
    }
  }
}
