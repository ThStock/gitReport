import ChangeTypes.{Contributor, VisibleChange, VisibleRepo}
import ChangeTypesSpec._
import org.scalatest.{FeatureSpec, GivenWhenThen}

class ChangeTypesSpec extends FeatureSpec with GivenWhenThen {

  feature("VisibleRepo") {
    scenario("changesPerDay - 0") {
      Given("no changes")
      val changes = Nil

      When("to repo")
      val repo = VisibleRepo("test", changes, Nil, 1)

      Then("check")
      assert(0 == repo.changesPerDay)
      assert(0 == repo.mainComitters)
    }

    scenario("changesPerDay - 1") {
      Given("one change")
      val changes = Seq(newVisChange("e"))

      When("to repo")
      val repo = VisibleRepo("test", changes, Nil, 1)

      Then("check")
      assert(1 == repo.changesPerDay)
      assert(1 == repo.mainComitters)
    }


    scenario("changesPerDay - 2") {
      Given("changes")
      val changes = Seq(newVisChange("e"), newVisChange("e"))

      When("to repo")
      val repo = VisibleRepo("test", changes, Nil, 1)

      Then("check")
      assert(2 == repo.changesPerDay)
      assert(1 == repo.mainComitters)
    }

    scenario("changesPerDay - 1.5") {
      Given("changes")
      val changes = Seq(newVisChange("e"), newVisChange("e"), newVisChange("a"))

      When("to repo")
      val repo = VisibleRepo("test", changes, Nil, 1)

      Then("check")
      assert(2 == repo.mainComitters)
      assert(1.5 == repo.changesPerDay)
    }

    scenario("changesPerDay - 2.66") {
      Given("changes")
      val changes = Seq.fill(1)(newVisChange("a")) ++ Seq.fill(2)(newVisChange("b")) ++
        Seq.fill(29)(newVisChange("c")) ++ Seq.fill(55)(newVisChange("d"))

      When("to repo")
      val repo = VisibleRepo("test", changes, Nil, 7)

      Then("check")
      assert(2.7 == repo.changesPerDay)
      assert(2 == repo.mainComitters)
    }

  }

}

object ChangeTypesSpec {

  private def newChange(repoName: String)(authorEmail: String) = {
    VisibleChange(author = Contributor(authorEmail + "@example.org", Contributor.AUTHOR), Nil, 0, "r1")
  }

  def newVisChangeOfRepo(repoName: String): (String ⇒ VisibleChange) = {
    newChange(repoName)
  }

  def newVisChange(authorEmail: String) = {
    newVisChangeOfRepo("r1")(authorEmail)
  }

}
