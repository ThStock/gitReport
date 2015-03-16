import org.scalatest.{FeatureSpec, GivenWhenThen}

class RepoGeneratorSpec extends FeatureSpec with GivenWhenThen {

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
