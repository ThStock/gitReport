import ChangeTypes.{Contributor, VisibleChangeStatus}
import RepoAnalyzer.{Change, FooterElement}
import org.scalatest._

class RepoAnalyzerSpec extends FeatureSpec with GivenWhenThen {

  feature("participation") {

    scenario("empty") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Nil, 1, 100, 0)

      Then("calculated")
      assert(result == Seq(0))
    }

    scenario("single") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1), 1, 100, 1)

      Then("calculated")
      assert(result == Seq(100))
    }

    scenario("1 of 4") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1), 4, 100, 400)

      Then("calculated")
      assert(result == Seq(100, 0, 0, 0))
    }

    scenario("2 of 6") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(210, 250), 6, 100, 534)

      Then("calculated")
      assert(result == Seq(0, 0, 100, 100, 0, 0))
    }

    scenario("3 of 2") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1, 50, 110), 2, 100, 110)

      Then("calculated")
      assert(result == Seq(50, 100))
    }

    scenario("3 of 4; 4") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1, 2, 2, 3, 3, 3, 4, 4, 4, 4), 3, 1, 4)

      Then("calculated")
      assert(result == Seq(50, 75, 100))
    }

    scenario("3 of 4; 5") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1, 2, 2, 3, 3, 3, 4, 4, 4, 4), 3, 1, 5)

      Then("calculated")
      assert(result == Seq(75, 100, 0))
    }

    scenario("3 of 4; 3") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1, 2, 2, 3, 3, 3, 4, 4, 4, 4), 3, 1, 3)

      Then("calculated")
      assert(result == Seq(33, 66, 100))
    }

    scenario("2 of 4; 3") {
      Given("a")
      When("calc")
      val result: Seq[Int] = RepoAnalyzer.calcParticipationPercentages(Seq(1, 2, 2, 3, 3, 3, 4, 4, 4, 4), 2, 1, 3)

      Then("calculated")
      assert(result == Seq(66, 100))
    }
  }

  feature("RepoAnalyser") {
    scenario("md5 calculation for gravatar icons") {
      Given("an emailaddress")
      val testvalue = "test@example.org"

      When("calc md5sum")
      val md5: String = RepoAnalyzer.md5(testvalue)

      Then("calculated")
      assert(md5 == "0c17bf66e649070167701d2d3cd71711")
    }
  }

  feature("FooterElement") {
    scenario("detect no footerlines") {
      Given("any text")
      val text = "test text value setter"

      When("find")
      val elements: Seq[FooterElement] = FooterElement.elementsIn(text)

      Then("not found")
      assert(Nil == elements)
    }

    scenario("detect footerlines") {
      Given("text with footerlines")
      val text =
        """
          |Header
          |
          |Line:value
        """.stripMargin

      When("find")
      val elements: Seq[FooterElement] = FooterElement.elementsIn(text)

      Then("found")
      assert(Seq(FooterElement("Line", "value")) == elements)
    }

    scenario("detect footerlines with spaces") {
      Given("text with footerlines")
      val text =
        """
          |Header
          |
          |Line: value
        """.stripMargin + " "

      When("find")
      val elements: Seq[FooterElement] = FooterElement.elementsIn(text)

      Then("found")
      assert(Seq(FooterElement("Line", "value")) == elements)
    }

    scenario("detect valid emailaddress") {
      Given("valid email")
      val key = "key"
      val text = "Someone <some@example.org>"

      When("find")
      val element: FooterElement = RepoAnalyzer.FooterElement(key, text)

      Then("found")
      assert(Some("some@example.org") == element.email)
    }

    scenario("detect no emailaddress") {
      Given("invalid email")
      val key = "key"
      val text = "some@example.org>"

      When("find")
      val element: FooterElement = RepoAnalyzer.FooterElement(key, text)

      Then("not found")
      assert(None == element.email)
    }
  }

  feature("to visible change") {
    scenario("convert commit with only an author") {
      Given("change with author")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "bert@example.org"
      val change = Change(authorEmail, "Bert", "Do Something", "4", 7000, Nil, true)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, "/home/a/" + repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(authorEmail)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(7000)(result.commitTimeMillis)
      assertResult("Time: 1970-01-01 00:00:07\nRepo: a")(result.title)
      assertResult(Nil)(result.contributors)
      assertResult(Seq(Contributor(authorEmail, Contributor.AUTHOR)))(result.members)
      assertResult(VisibleChangeStatus.warn)(result.changeStatus)
      assertResult("warn")(result.color)
    }

    scenario("convert commit with reviewers") {
      Given("change with author and reviewers")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val repoName = "a"
      val authorEmail: String = "Bert@example.org"
      val reviewerEmail = "Reviewer@Example.org"
      val authorsToEmails = Map[String, String]()
      val others: Seq[FooterElement] = Seq(FooterElement("Code-Review", "Some"),
        FooterElement("Code-Review", "Random J Developer <" + reviewerEmail + ">")
      )
      val change = Change(authorEmail, "Bert", "Do Something", "41", 11000, others, true)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, "/home/a/" + repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(authorEmail.toLowerCase)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(11000)(result.commitTimeMillis)
      assertResult("Time: 1970-01-01 00:00:11\nRepo: a")(result.title)

      val expectedContributors: Seq[Contributor] = //
        Seq(Contributor("Some", Contributor.REVIWER), Contributor(reviewerEmail.toLowerCase, Contributor.REVIWER))
      assert(expectedContributors == result.contributors)

      val expectedMembers: Seq[Contributor] = Seq(//
        Contributor("Some", Contributor.REVIWER),
        Contributor(reviewerEmail.toLowerCase, Contributor.REVIWER),
        Contributor(authorEmail.toLowerCase, Contributor.AUTHOR)
      )
      assert(expectedMembers == result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }

    scenario("convert commit with signer") {
      Given("change with author and signer")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "Bert@example.org"
      val signerEmail: String = "Random@developer.example.org"
      val others: Seq[FooterElement] = Seq(FooterElement("Signed-off-by", "Random J Developer <" + signerEmail + ">"))
      val change = Change(authorEmail, "Bert", "Do Something", "41", 81000, others, true)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, "/home/a/" + repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(signerEmail.toLowerCase)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(81000)(result.commitTimeMillis)
      assertResult("Time: 1970-01-01 00:01:21\nRepo: a")(result.title)
      assertResult(Seq(Contributor(authorEmail.toLowerCase, Contributor.REVIWER)))(result.contributors)
      val expectedMembers: Seq[Contributor] = //
        Seq(Contributor(authorEmail.toLowerCase, Contributor.REVIWER), //
          Contributor(signerEmail.toLowerCase, Contributor.AUTHOR) //
        )
      assert(expectedMembers == result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }

    scenario("convert commit with two signers") {
      Given("change with author and signer")
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "Bert@example.org"
      val signerEmail: String = "Random@developer.example.org"
      val others: Seq[FooterElement] = Seq(FooterElement.signer("Random J Developer", signerEmail),
        FooterElement.signer("Bert", authorEmail)
      )
      val change = Change(authorEmail, "Bert", "Do Something", "41", 81, others, true)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, "/home/a/" + repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(signerEmail.toLowerCase)(result.author.email)
      assertResult("author")(result.author.typ)
      assertResult(Seq(Contributor(authorEmail.toLowerCase, Contributor.AUTHOR)))(result.contributors)
      val expectedMembers: Seq[Contributor] = //
        Seq(Contributor(authorEmail.toLowerCase, Contributor.AUTHOR),
          Contributor(signerEmail.toLowerCase, Contributor.AUTHOR) //
        )
      assert(expectedMembers == result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }

    scenario("convert commit with signer - no personal details") {
      Given("change with author and signer")
      java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "Bert@example.org"
      val signerEmail: String = "Random@developer.example.org"
      val others: Seq[FooterElement] = Seq(FooterElement("Signed-off-by", "Random J Developer <" + signerEmail + ">"))
      val change = Change(authorEmail, "Bert", "Do Something", "41", 81000, others, false)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, "/home/a/" + repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult("some@example.org")(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(81000)(result.commitTimeMillis)
      assertResult("Time: 1970-01-01 00:01:21\nRepo: a")(result.title)
      assertResult(Seq(Contributor("other@example.org", Contributor.REVIWER)))(result.contributors)
      assertResult(Seq(Contributor("other@example.org", Contributor.REVIWER), Contributor("some@example.org", Contributor.AUTHOR)
      )
      )(result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }
  }
}
