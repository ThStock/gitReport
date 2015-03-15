import ChangeTypes.{Contributor, VisibleChangeStatus}
import RepoAnalyzer.{Change, FooterElement}
import org.scalatest._

class RepoAnalyzerSpec extends FeatureSpec with GivenWhenThen {

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
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "bert@example.org"
      val change = Change(authorEmail, "Bert", "Do Something", "4", 7)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(authorEmail)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(7)(result.commitTime)
      assertResult("Time: 1970-01-01 01:00:07\nRepo: a")(result.title)
      assertResult(Nil)(result.contributors)
      assertResult(Seq(Contributor(authorEmail, "author")))(result.members)
      assertResult(VisibleChangeStatus.warn)(result.changeStatus)
      assertResult("warn")(result.color)
    }

    scenario("convert commit with reviewers") {
      Given("change with author and reviewers")
      val repoName = "a"
      val authorEmail: String = "Bert@example.org"
      val reviewerEmail = "Reviewer@Example.org"
      val authorsToEmails = Map[String, String]()
      val others: Seq[FooterElement] = Seq(
        FooterElement("Code-Review", "Some"),
        FooterElement("Code-Review", "Random J Developer <" + reviewerEmail + ">")
      )
      val change = Change(authorEmail, "Bert", "Do Something", "41", 11, others)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(authorEmail.toLowerCase)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(11)(result.commitTime)
      assertResult("Time: 1970-01-01 01:00:11\nRepo: a")(result.title)
      assertResult(Seq(Contributor("Some", "Code-Review"),
        Contributor(reviewerEmail.toLowerCase, "Code-Review")))(result.contributors)
      assertResult(Seq(Contributor("Some", "Code-Review"), Contributor(reviewerEmail.toLowerCase, "Code-Review"),
        Contributor(authorEmail.toLowerCase, "author")))(result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }
    scenario("convert commit with signer") {
      Given("change with author and signer")
      val repoName = "a"
      val authorsToEmails = Map[String, String]()
      val authorEmail: String = "Bert@example.org"
      val signerEmail: String = "Random@developer.example.org"
      val others: Seq[FooterElement] = Seq(FooterElement("Signed-off-by", "Random J Developer <" + signerEmail + ">"))
      val change = Change(authorEmail, "Bert", "Do Something", "41", 81, others)

      When("convert")
      val result = RepoAnalyzer.toVisChange(repoName, authorsToEmails)(change)

      Then("compare")
      assertResult(repoName)(result.repoName)
      assertResult(signerEmail.toLowerCase)(result.author.email)
      assertResult(true)(result.author.isAuthor)
      assertResult("author")(result.author.typ)
      assertResult(81)(result.commitTime)
      assertResult("Time: 1970-01-01 01:01:21\nRepo: a")(result.title)
      assertResult(Seq(Contributor(authorEmail.toLowerCase, "Code-Review")))(result.contributors)
      assertResult(Seq(Contributor(authorEmail.toLowerCase, "Code-Review"), Contributor(signerEmail.toLowerCase, "author")))(result.members)
      assertResult(VisibleChangeStatus.ok)(result.changeStatus)
      assertResult("ok")(result.color)
    }
  }
}
