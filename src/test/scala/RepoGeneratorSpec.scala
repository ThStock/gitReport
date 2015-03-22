import ReportGenerator.ActivityScore
import org.scalatest.{FeatureSpec, GivenWhenThen}

class RepoGeneratorSpec extends FeatureSpec with GivenWhenThen {

  feature("activity score") {
    scenario("invalid params") {
      Given("no changes")
      val map = Map[String, Seq[ChangeTypes.VisibleChange]]()

      When("get score")
      val exception = intercept[NoSuchElementException] {
        ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      }

      Then("check")
      assertResult("None.get")(exception.getMessage)
    }

    scenario("zero") {
      Given("no changes")
      val map = Map("a" -> Nil)

      When("get score")
      val exception = intercept[NoSuchElementException] {
        ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      }
      Then("check")
      assertResult("key not found: 0")(exception.getMessage)

    }

    scenario("single change with one change") {
      Given("changes")
      val change = ChangeTypesSpec.newVisChange("any")
      val map = Map("a" -> Seq(change))

      When("get score")
      val result = ReportGenerator.repoActivityScoreOf(repoName = "a", map)

      Then("check")
      assertResult(ActivityScore.high)(result)
    }

    scenario("2 with 3,2") {
      Given("changes")

      val chA = ChangeTypesSpec.newVisChangeOfRepo("a")("any")
      val chB = ChangeTypesSpec.newVisChangeOfRepo("b")("any")

      val map = Map("a" → Seq(chA, chA, chA), "b" → Seq(chB, chB))

      When("get score")
      val resultA = ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      val resultB = ReportGenerator.repoActivityScoreOf(repoName = "b", map)

      Then("check")
      assertResult(ActivityScore.high)(resultA)
      assertResult(ActivityScore.mid)(resultB)
    }

    scenario("4 with 100,3,2,1") {
      Given("changes")

      val chA = ChangeTypesSpec.newVisChangeOfRepo("a")("any")
      val chB = ChangeTypesSpec.newVisChangeOfRepo("b")("any")
      val chC = ChangeTypesSpec.newVisChangeOfRepo("c")("any")
      val chD = ChangeTypesSpec.newVisChangeOfRepo("d")("any")

      val map = Map("b" → Seq.fill(3)(chB), "c" → Seq.fill(2)(chC), "d" → Seq.fill(1)(chD), "a" → Seq.fill(100)(chA))

      When("get score")
      val resultA = ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      val resultB = ReportGenerator.repoActivityScoreOf(repoName = "b", map)
      val resultC = ReportGenerator.repoActivityScoreOf(repoName = "c", map)
      val resultD = ReportGenerator.repoActivityScoreOf(repoName = "d", map)

      Then("check")
      assertResult(ActivityScore.high)(resultA)
      assertResult(ActivityScore.high)(resultB)
      assertResult(ActivityScore.mid)(resultC)
      assertResult(ActivityScore.low)(resultD)
    }

    scenario("3 with 3,2,1") {
      Given("changes")

      val chA = ChangeTypesSpec.newVisChangeOfRepo("a")("any")
      val chB = ChangeTypesSpec.newVisChangeOfRepo("b")("any")
      val chC = ChangeTypesSpec.newVisChangeOfRepo("c")("any")

      val map = Map("a" → Seq(chA, chA, chA), "b" → Seq(chB, chB), "c" → Seq(chC))

      When("get score")
      val resultA = ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      val resultB = ReportGenerator.repoActivityScoreOf(repoName = "b", map)
      val resultC = ReportGenerator.repoActivityScoreOf(repoName = "c", map)

      Then("check")
      assertResult(ActivityScore.high)(resultA)
      assertResult(ActivityScore.mid)(resultB)
      assertResult(ActivityScore.low)(resultC)

    }

    scenario("3 with 3,3,2") {
      Given("changes")

      val chA = ChangeTypesSpec.newVisChangeOfRepo("a")("any")
      val chB = ChangeTypesSpec.newVisChangeOfRepo("b")("any")
      val chC = ChangeTypesSpec.newVisChangeOfRepo("c")("any")

      val map = Map("a" → Seq(chA, chA, chA), "b" → Seq(chB, chB, chB), "c" → Seq(chC))

      When("get score")
      val resultA = ReportGenerator.repoActivityScoreOf(repoName = "a", map)
      val resultB = ReportGenerator.repoActivityScoreOf(repoName = "b", map)
      val resultC = ReportGenerator.repoActivityScoreOf(repoName = "c", map)

      Then("check")
      assertResult(ActivityScore.high)(resultA)
      assertResult(ActivityScore.high)(resultB)
      assertResult(ActivityScore.mid)(resultC)

    }
  }

  feature("slidings") {
    scenario("3 elments in 3") {
      Given("a,b,c")
      val ints: Seq[String] = Seq("a", "b", "c")

      When("sliding")
      val result: Seq[Seq[String]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq("a"), Seq("b"), Seq("c")))(result)
    }

    scenario("4 elments in 3") {
      Given("4,2,3,1")
      val ints: Seq[Int] = Seq(4, 2, 3, 1)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(4, 2), Seq(3), Seq(1)))(result)
    }

    scenario("6 elments in 3") {
      Given("4,2,3,1,5,8")
      val ints: Seq[Int] = Seq(4, 2, 3, 1, 5, 8)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(4, 2), Seq(3, 1), Seq(5, 8)))(result)
    }

    scenario("5 elments in 3") {
      Given("4,2,3,1,5")
      val ints: Seq[Int] = Seq(4, 2, 3, 1, 5)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(4, 2), Seq(3, 1), Seq(5)))(result)
    }

    scenario("7 elments in 3") {
      Given("4,2,3,1,5,8,9")
      val ints: Seq[Int] = Seq(4, 2, 3, 1, 5, 8, 9)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(4, 2, 3), Seq(1, 5), Seq(8, 9)))(result)
    }

    scenario("8 elments in 3") {
      Given("4,2,3,1,5,8,9,0")
      val ints: Seq[Int] = Seq(4, 2, 3, 1, 5, 8, 9, 0)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(4, 2, 3), Seq(1, 5, 8), Seq(9, 0)))(result)
    }

    scenario("1 elments in 3") {
      Given("1")
      val ints: Seq[Int] = Seq(1)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(1), Nil, Nil))(result)
    }

    scenario("1 elments in 2") {
      Given("1")
      val ints: Seq[Int] = Seq(1)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(2)(ints)
      Then("check")

      assertResult(Seq(Seq(1), Nil))(result)
    }

    scenario("2 elments in 3") {
      Given("1,1")
      val ints: Seq[Int] = Seq(1, 1)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(3)(ints)
      Then("check")

      assertResult(Seq(Seq(1), Seq(1), Nil))(result)
    }

    scenario("2 elments in 5") {
      Given("1,1")
      val ints: Seq[Int] = Seq(1, 1)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(5)(ints)
      Then("check")

      assertResult(Seq(Seq(1), Seq(1), Nil, Nil, Nil))(result)
    }

    scenario("1 elments in 1") {
      Given("5")
      val ints: Seq[Int] = Seq(5)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(1)(ints)
      Then("check")

      assertResult(Seq(Seq(5)))(result)
    }

    scenario("2 elments in 4") {
      Given("1,7")
      val ints: Seq[Int] = Seq(1, 7)

      When("sliding")
      val result: Seq[Seq[Int]] = ReportGenerator.slidingsOf(4)(ints)
      Then("check")

      assertResult(Seq(Seq(1), Seq(7), Nil, Nil))(result)
    }
  }

}
