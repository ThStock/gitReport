import ChangeTypes.Contributor.ContributorActivity

object ChangeTypes {

  trait VisibleChangeT {
    def author: Contributor
    val commitTime: Int
    def repoName: String
    def repoFullPath: String
    def changeStatus(): VisibleChangeStatus
    val highlightPersonalExchange: Boolean
    def contributors: Seq[Contributor]
    def members:Seq[Contributor]
  }
  case class VisibleChange(author: Contributor,
                           contributors: Seq[Contributor],
                           commitTime: Int,
                           repoName: String,
                           repoFullPath: String,
                           highlightPersonalExchange: Boolean) extends VisibleChangeT {

    val members = contributors :+ author

    private def authorIsContributor(author: Contributor)(all: Seq[Contributor]): Boolean = {
      val review = all.filter(_.typ.startsWith("Code-Review")).map(_.email)
      review.contains(author.email)
    }

    def color = changeStatus match {
      case VisibleChangeStatus.warn => "warn"
      case VisibleChangeStatus.ok => "ok"
      case _ => "warn"
    }

    private def formatDate(date: Int) = ReportGenerator.formatedDateBySecs(date)

    val title = """|
                  |Time: %s
                  |Repo: %s
                  | """.stripMargin.trim.format(formatDate(commitTime), repoName)

    def changeStatus = members match {
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
                         noGerrit: Boolean = false) {
    val hash = RepoAnalyzer.md5(email)
    val isAuthor = _typ.isAuthor
    val activityValue = activity.key
    val typ: String = _typ.name

  }

  object Contributor {
    val AUTHOR: ContributorType = ContributorType("author")
    val REVIWER = ContributorType("Code-Review")

    case class ContributorActivity(key: String)

    object ContributorActivity {
      val LOWEST = ContributorActivity("lowest")
      val LOW = ContributorActivity("low")
      val MID = ContributorActivity("mid")
      val HIGH = ContributorActivity("high")
      val HIGHEST = ContributorActivity("highest")
    }

  }

  trait VisibleRepoT {
    def changes: Seq[VisibleChangeT]
    def repoName: String
    def repoFullPath: String
    def branchNames: Seq[String]
  }

  case class VisibleRepo(repoName: String,
                        repoFullPath: String,
                         _changes: Seq[VisibleChangeT],
                         branchNames: Seq[String],
                         _sprintLengthInDays: Int,
                         _activity: Int = 0,
                         topComitter: Boolean = false) extends VisibleRepoT {
    repoFullPath.getClass // XXX null check

    val activityIndex = _activity match {
      case i if i > 1 => "high"
      case i if i < 1 => "low"
      case _ => "normal"
    }

    val allChangesCount: Int = _changes.size

    val branchCount = branchNames.size

    val branchNamesText = branchNames.map(_.replaceFirst("refs/heads/", "")).mkString("\n")

    val branchCountOk = branchCount <= 2

    val okChangesCount: Int = _changes.count(_.changeStatus == VisibleChangeStatus.ok)

    def percentageOk(): Int = {
      val result: Double = okChangesCount.toDouble / allChangesCount.toDouble * 100
      result.toInt
    }

    val percentageOkGt66 = percentageOk() > 66

    val percentageOkGt80 = percentageOk() > 80

    val noGerrit = percentageOk() == 0 && _changes.count(_.contributors == Nil) == _changes.size

    val changes = _changes

    val members: Seq[Contributor] = VisibleRepo.toContibutors(changes).map(_.copy(noGerrit = this.noGerrit))

    private val changeCountsByAuthor = _changes.groupBy(_.author).toSeq.map(_._2.size)

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

    val mainComitters: Int = {
      if (changes.exists(_.highlightPersonalExchange)) {
        val devs = changeCountsByAuthor.map(score => Math.pow(score - changesPerDay, 2))
        val stddev = Math.sqrt(devs.sum / devs.size)
        changeCountsByAuthor.count(_ >= stddev.toInt)
      } else {
        0
      }
    }

  }

  object VisibleRepo {
    def toContibutors(changes: Seq[VisibleChangeT]): Seq[ChangeTypes.Contributor] = {
      object EmailAndTyp {
        def by(contributor: Contributor) = EmailAndTyp(contributor.email, "")
      }
      case class EmailAndTyp(email: String, typ: String)

      val allMembers = changes.flatMap(_.members)
      val allMembersEmails = allMembers.map(_.email).toSet
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
          val changesOfContibutors = changesOf.map(_.contributors).map(in => in.map(EmailAndTyp.by))
          val selfReviews = changesOfContibutors.filter(_.contains(self))
          val contributorsSimpyfied = changesOf.flatMap(_.contributors).map(EmailAndTyp.by)
          val contributions = allChangesByContributor.get(EmailAndTyp.by(contributor))

          val selfReviewsVsChanges = selfReviews.size.toDouble / (changesOfContibutors.size + contributions
            .getOrElse(Nil)
            .size - selfReviews.size)
          val isNoDirectCommit = contributorsSimpyfied.size >= changesOf.size
          val repoHasMoreThenOneMember = membersSimpyfied.toSet.size > 1
          val repoHasMoreThenOneContributors = contributorsSimpyfied.toSet.size > 1

          if (allMembersEmails.size > 1 && membersSimpyfied.toSet.size == allMembersEmails.size &&
            selfReviewsVsChanges < 0.1d && isNoDirectCommit) {
            ContributorActivity.HIGHEST
          } else if (allMembersEmails.size > 1 && selfReviewsVsChanges < 0.2d && isNoDirectCommit) {
            ContributorActivity.HIGH
          } else if (allMembersEmails.size == 1) {
            ContributorActivity.MID
          } else if (repoHasMoreThenOneMember && selfReviewsVsChanges < 0.4d && isNoDirectCommit) {
            ContributorActivity.MID
          } else if (repoHasMoreThenOneContributors && selfReviewsVsChanges < 0.8d) {
            ContributorActivity.LOW
          } else {
            ContributorActivity.LOWEST
          }
        }
      }

      allMembers.map(in => in.copy(_typ = ContributorType("player"))).toSet[Contributor]
        .map(in => in.copy(activity = selectActivity(in)))
        .toSeq
        .sortWith(_.email < _.email)
    }

  }

}
