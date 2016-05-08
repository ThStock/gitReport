import ChangeTypes._
import ReportGenerator.{ActivityScore, Segmented, Slot}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FeatureSpec, GivenWhenThen}
import java.util.Date

class RepoGeneratorSpec extends FeatureSpec with GivenWhenThen with MockFactory {

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

  feature("write") {
    scenario("empty") {
      Given("a")
      val now = new Date()
      val repos: Seq[VisibleRepoT] = Nil
      val diskIo = mock[ReportGenerator.DiskIoT]

      (diskIo.copyToOutputFolder _).expects("octoicons/octicons.css")
      (diskIo.copyToOutputFolder _).expects("octoicons/octicons.eot")
      (diskIo.copyToOutputFolder _).expects("octoicons/octicons.svg")
      (diskIo.copyToOutputFolder _).expects("octoicons/octicons.woff")
      (diskIo.copyToOutputFolder _).expects("bootstrap-3.3.6-dist/css/bootstrap.min.css")
      (diskIo.copyToOutputFolder _).expects("git-report-xs.png")
      (diskIo.copyToOutputFolder _).expects("git-report.svg")

      When("write")
      new ReportGenerator(repos).writeTruckByRepo(0, repos.flatMap(_.changes), 1, 1, diskIo, now)

      Then("check")
    }

    scenario("single") {
      Given("a")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val now = new Date()
      val repo = stub[VisibleRepoT]
      (repo.participationPercentages _).when().returning(Nil).once()
      (repo.badges _).when().returning(Nil).once()
      val repos = Seq(repo)
      val diskIo = mock[ReportGenerator.DiskIoT]
      val change = stub[VisibleChangeT]
      val author = Contributor("q@example.org", Contributor.AUTHOR)
      (change.members _).when().returning(Seq(author)).repeat(6)
      (change.contributors _).when().returning(Seq(author)).repeat(12)
      (change.author _).when().returning(author).repeat(9)
      (change.repoName _).when().returning("repoName").once()
      (change.repoFullPath _).when().returning("/home/git/r/repoName").twice()

      val visRepo = newRepo("r", change, "master")
      val o = Segmented(Seq(Slot(Seq(visRepo)), Slot(Nil), Slot(Nil)), "1970-01-01 00:00:00", "1970-01-01 00:00:00", 1)
      (diskIo.writeByNameToDisk _).expects("truckByProject", o, now, "index").once()
      (diskIo.copyToOutputFolder _).expects(*).anyNumberOfTimes()
      (repo.changes _).when().returning(Seq(change)).once()
      (repo.branchNames _).when().returning(Seq("master")).once()
      (repo.repoFullPath _).when().returning("/home/git/r/repoName").once() // TODO missing

      When("write")
      new ReportGenerator(repos).writeTruckByRepo(0, repos.flatMap(_.changes), 10, 1, diskIo, now)

      Then("check")
    }

    scenario("two") {
      Given("a")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val now = new Date()
      val diskIo = mock[ReportGenerator.DiskIoT]
      val author = Contributor("q@example.org", Contributor.AUTHOR)
      val changeA = stub[VisibleChangeT]
      (changeA.members _).when().returning(Seq(author))
      (changeA.contributors _).when().returning(Seq(author))
      (changeA.author _).when().returning(author)
      (changeA.repoName _).when().returning("repoName").once()
      (changeA.repoFullPath _).when().returning("/home/git/a/repoName").anyNumberOfTimes()

      val changeB = stub[VisibleChangeT]
      (changeB.members _).when().returning(Seq(author))
      (changeB.contributors _).when().returning(Seq(author))
      (changeB.author _).when().returning(author)
      (changeB.repoName _).when().returning("repoName").once()
      (changeB.repoFullPath _).when().returning("/home/git/b/repoName").anyNumberOfTimes()

      val visRepoA = newRepo("a", changeA, "master")
      val visRepoB = newRepo("b", changeB, "develop")
      val o = Segmented(Seq(Slot(Seq(visRepoA)), Slot(Seq(visRepoB)), Slot(Nil)), "1970-01-01 00:00:00", "1970-01-01 00:00:00", 1)
      (diskIo.writeByNameToDisk _).expects("truckByProject", o, now, "index").once()
      (diskIo.copyToOutputFolder _).expects(*).anyNumberOfTimes()

      val repoA = stub[VisibleRepoT]
      (repoA.participationPercentages _).when().returning(Nil).once()
      (repoA.badges _).when().returning(Nil).once()
      val repoB = stub[VisibleRepoT]
      (repoB.participationPercentages _).when().returning(Nil).once()
      (repoB.badges _).when().returning(Nil).once()
      val repos = Seq(repoA, repoB)

      (repoA.changes _).when().returning(Seq(changeA)).once()
      (repoA.branchNames _).when().returning(Seq("master")).once()
      (repoA.repoFullPath _).when().returning("/home/git/a/repoName").anyNumberOfTimes()

      (repoB.changes _).when().returning(Seq(changeB)).once()
      (repoB.branchNames _).when().returning(Seq("develop")).once()
      (repoB.repoFullPath _).when().returning("/home/git/b/repoName").anyNumberOfTimes()

      When("write")
      new ReportGenerator(repos).writeTruckByRepo(0, repos.flatMap(_.changes), 10, 1, diskIo, now)

      Then("check")
    }
  }

  def newRepo(folderPart: String, change: VisibleChangeT, branchName: String) = {
    VisibleRepo("repoName", "/home/git/" + folderPart + "/repoName", Seq(change), Seq(branchName), 1, Nil, Nil, 2, true)
  }

}
