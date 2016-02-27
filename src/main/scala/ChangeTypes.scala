import javax.management.MXBean

import ChangeTypes.Contributor.ContributorActivity

import scala.beans.BeanProperty

object ChangeTypes {

  trait VisibleChangeT {
    def author: Contributor
    val commitTimeMillis: Long
    def repoName: String
    def repoFullPath: String
    def changeStatus(): VisibleChangeStatus
    val highlightPersonalExchange: Boolean
    def contributors: Seq[Contributor]
    def members:Seq[Contributor]
  }
  case class VisibleChange(author: Contributor,
                           contributors: Seq[Contributor],
                           commitTimeMillis: Long,
                           repoName: String,
                           repoFullPath: String,
                           highlightPersonalExchange: Boolean) extends VisibleChangeT {

    val members = contributors :+ author

    private def authorIsContributor(author: Contributor)(all: Seq[Contributor]): Boolean = {
      val review = all.filter(_.typ.startsWith("Code-Review")).map(_.email)
      review.contains(author.email)
    }

    def color = changeStatus() match {
      case VisibleChangeStatus.warn => "warn"
      case VisibleChangeStatus.ok => "ok"
      case _ => "warn"
    }

    private def formatDate(date: Long) = ReportGenerator.formatedDateByMillis(date)

    val title = """|
                  |Time: %s
                  |Repo: %s
                  | """.stripMargin.trim.format(formatDate(commitTimeMillis), repoName)

    def changeStatus() = members match {
      case c if c.size == 1 => VisibleChangeStatus.warn
      case c if authorIsContributor(author)(c) => VisibleChangeStatus.warn
      case c if c.size >= 2 => VisibleChangeStatus.ok
      case _ => VisibleChangeStatus.warn
    }
  }

  case class VisibleChangeStatus(key: String)

  object VisibleChangeStatus {
    val warn = VisibleChangeStatus("warning")
    val ok = VisibleChangeStatus("ok")
  }

  case class ContributorType(name: String) {
    val isAuthor = name == "author"
  }

  case class Contributor(email: String,
                         _typ: ContributorType,
                         activity: Contributor.ContributorActivity = ContributorActivity.LOWEST,
                         noGerrit: Boolean = false, isMainComitter: Boolean = false) {
    val hash = RepoAnalyzer.md5(email)
    val isAuthor = _typ.isAuthor
    val activityValue = activity.key
    val activityReason = activity.reason
    val typ: String = _typ.name

    def copyAsAuthor() = copy(_typ = Contributor.AUTHOR)

  }

  object Contributor {
    val AUTHOR: ContributorType = ContributorType("author")
    val REVIWER = ContributorType("Code-Review")

    case class ContributorActivity(key: String, _reason:String = "") {
      def reason = _reason
    }

    object ContributorActivity {
      val LOWEST = ContributorActivity("lowest")
        .copy(_reason = "almost no exchange")
      val LOW = ContributorActivity("low")
        .copy(_reason = "low exchange or direct commit")
      val MID = ContributorActivity("mid")
        .copy(_reason = "medium review rate")
      val HIGH = ContributorActivity("high")
        .copy(_reason = "")
      val HIGHEST = ContributorActivity("highest")
        .copy(_reason = "maximum")
    }

  }

  case class ParticipationBar(width:Int, height:Int, x:Int)

  trait VisibleRepoT {
    def changes: Seq[VisibleChangeT]
    def repoName: String
    def repoFullPath: String
    def branchNames: Seq[String]
    def participationBars():Seq[ParticipationBar]
    def participationPercentages:Seq[Int]
    def badges:Seq[VisBadge]
  }

  case class VisibleRepo(repoName: String,
                         repoFullPath: String,
                         _changes: Seq[VisibleChangeT],
                         branchNames: Seq[String],
                         _sprintLengthInDays: Int,
                         participationPercentages:Seq[Int],
                         _badges:Seq[VisBadge],
                         _activity: Int = 0,
                         topComitter: Boolean = false) extends VisibleRepoT {
    repoFullPath.getClass // XXX null check

    val badges = _badges

    val activityIndex = _activity match {
      case i if i > 1 => "high"
      case i if i < 1 => "low"
      case _ => "normal"
    }

    def participationBars() = {
      val xshift = 7
      Seq.tabulate(14)(i ⇒ ParticipationBar(width = 5, height = 100, x = 100 -xshift - xshift * i))
        .zip(participationPercentages.reverse).map(in ⇒ in._1.copy(height = in._2))
    }

    val allChangesCount: Int = _changes.size

    val branchCount = branchNames.size

    val branchNamesText = branchNames.map(_.replaceFirst("refs/heads/", "")).mkString("\n")

    val branchCountOk = branchCount <= 2

    val okChangesCount: Int = VisibleRepo.okChanges(_changes)

    def percentageOk(): Int = {
      VisibleRepo.percentageOk(_changes)
    }

    val percentageOkGt66 = percentageOk() > 66

    val percentageOkGt80 = percentageOk() > 80

    val noGerrit = percentageOk() == 0 && _changes.count(_.contributors == Nil) == _changes.size

    val changes = _changes

    val members: Seq[Contributor] = VisibleRepo.toContibutors(changes).map(_.copy(noGerrit = this.noGerrit))
      .map(applyMainCommiter)

    private def changesByAuthor = _changes.groupBy(_.author).toSeq

    private def changeCountsByAuthor = changesByAuthor.map(_._2.size)

    val changesPerDay: Double = {
      if (changes.exists(_.highlightPersonalExchange)) {
        val medianChanges = ReportGenerator.median(changeCountsByAuthor)
        val meanChanges: Double = if (_changes == Nil) {
          0d
        } else {
          allChangesCount.toDouble / changeCountsByAuthor.size.toDouble
        }
        val result = (medianChanges + meanChanges) / 2d / _sprintLengthInDays
        BigDecimal(result).setScale(1, BigDecimal.RoundingMode.HALF_UP).toDouble
      } else {
        0d
      }
    }

    private lazy val mainCommiterMembers:Seq[Contributor] = {
      val devs = changeCountsByAuthor.map(score => Math.pow(score - changesPerDay, 2))
      val stddev = Math.sqrt(devs.sum / devs.size)
      changesByAuthor.filter(_._2.size >= stddev.toInt).map(_._1)
    }

    val mainComitters: Int = {
      if (changes.exists(_.highlightPersonalExchange)) {
        mainCommiterMembers.size
      } else {
        0
      }
    }

    private def applyMainCommiter(contributor: Contributor) = {
      if (mainCommiterMembers.map(_.email).contains(contributor.email)) {
        contributor.copy(isMainComitter = true)
      } else {
        contributor
      }
    }
  }

  object VisibleRepo {

    def okChanges(changes:Seq[VisibleChangeT]) = {
      changes.count(_.changeStatus == VisibleChangeStatus.ok)
    }

    def percentageOk(changes:Seq[VisibleChangeT]):Int = {
      val result: Double = okChanges(changes).toDouble / changes.size.toDouble * 100
      result.toInt
    }

    def toContibutors(changes: Seq[VisibleChangeT]): Seq[ChangeTypes.Contributor] = {
      object EmailAndTyp {
        def by(contributor: Contributor) = EmailAndTyp(contributor.email, "")
      }
      case class EmailAndTyp(email: String, typ: String)

      val allMembers = changes.flatMap(_.members)
      val allMembersEmails = allMembers.filter(_.isAuthor).map(_.email).toSet
      val allChangesByAuthor = changes.groupBy(_.author.copy(_typ = ContributorType("player")))
      val allChangesByContributor = changes.groupBy(_.contributors).flatMap(in => {
        in._1.map(key => (EmailAndTyp.by(key), in._2))
      })

      def selectActivity(contributor: Contributor) = {
        if (!allChangesByAuthor.contains(contributor)) {
          // TODO
          // is reviewer only
          ContributorActivity.HIGH
        } else {
          val self = EmailAndTyp(contributor.email, "")
          val changesOf = allChangesByAuthor(contributor)
          val membersSimpyfied = changesOf.flatMap(_.members).map(EmailAndTyp.by)
          val changesOfContibutors = changesOf.map(_.contributors.filterNot(_.isAuthor))
            .map(in => in.map(EmailAndTyp.by))
          val selfReviews = changesOfContibutors.filter(_.contains(self))
          val contributorsSimpyfied = changesOf.flatMap(_.contributors).map(EmailAndTyp.by)
          val contributions = allChangesByContributor.get(EmailAndTyp.by(contributor))

          val selfReviewsVsChanges = selfReviews.size.toDouble / (changesOfContibutors.size + contributions
            .getOrElse(Nil)
            .size - selfReviews.size)
          val isNoDirectCommit = contributorsSimpyfied.size >= changesOf.size
          val repoHasMoreThenOneMember = membersSimpyfied.toSet.size > 1
          val repoHasMoreThenOneContributors = contributorsSimpyfied.toSet.size > 1

          val exchangeWithTeam = allMembersEmails.size.toDouble / membersSimpyfied.toSet.size.toDouble
          val exchangeWithHalfOfTeam = exchangeWithTeam < 2

          if (allMembersEmails.size > 1 && exchangeWithHalfOfTeam && selfReviewsVsChanges < 0.1d && isNoDirectCommit) {
            ContributorActivity.HIGHEST
          } else if (allMembersEmails.size > 1 && selfReviewsVsChanges < 0.2d && isNoDirectCommit) {
            ContributorActivity.HIGH
          } else if (allMembersEmails.size == 1) {
            ContributorActivity.MID.copy(_reason = "only one player")
          } else if (repoHasMoreThenOneMember && selfReviewsVsChanges < 0.4d && isNoDirectCommit) {
            ContributorActivity.MID
          } else if (repoHasMoreThenOneContributors && selfReviewsVsChanges < 0.8d) {
            ContributorActivity.LOW
          } else {
            ContributorActivity.LOWEST
          }
        }
      }

      allMembers
        .map(member => member.copy(_typ = ContributorType("player")))
        .distinct
        .map(member => member.copy(activity = selectActivity(member)))
        .sortWith(_.email < _.email)
    }

  }

  object VisBadge {

    def moreReviews50(interations:Int, percentage:Int) =
      VisBadge("dashboard", "low-green", "More then 50%% (%s%%) reviews for over %s iterations".format(percentage, interations))


    def moreReviews60(interations:Int, percentage:Int) =
      VisBadge("dashboard", "green", "More then 60%% (%s%%) reviews for over %s iterations".format(percentage, interations))

    def moreReviews80(interations:Int, percentage:Int) =
      VisBadge("dashboard", "gold", "More then 80%% (%s%%) reviews for over %s iterations".format(percentage, interations))
  }

  case class VisBadge(_key:String, _color:String, _msg:String) {
    val key = _key
    val color = _color
    val msg = _msg
  }

}
