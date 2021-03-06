package app

import jp.sf.amateras.scalatra.forms._

import service._
import util.{CollaboratorsAuthenticator, ReferrerAuthenticator, UsersAuthenticator}

class MilestonesController extends MilestonesControllerBase
  with MilestonesService with RepositoryService with AccountService
  with ReferrerAuthenticator with CollaboratorsAuthenticator

trait MilestonesControllerBase extends ControllerBase {
  self: MilestonesService with RepositoryService
    with ReferrerAuthenticator with CollaboratorsAuthenticator  =>

  case class MilestoneForm(title: String, description: Option[String], dueDate: Option[java.util.Date])

  val milestoneForm = mapping(
    "title"       -> trim(label("Title", text(required, maxlength(100)))),
    "description" -> trim(label("Description", optional(text()))),
    "dueDate"     -> trim(label("Due Date", optional(date())))
  )(MilestoneForm.apply)

  get("/:owner/:repository/issues/milestones")(referrersOnly { repository =>
    issues.milestones.html.list(
      params.getOrElse("state", "open"),
      getMilestonesWithIssueCount(repository.owner, repository.name),
      repository,
      hasWritePermission(repository.owner, repository.name, context.loginAccount))
  })

  get("/:owner/:repository/issues/milestones/new")(collaboratorsOnly {
    issues.milestones.html.edit(None, _)
  })

  post("/:owner/:repository/issues/milestones/new", milestoneForm)(collaboratorsOnly { (form, repository) =>
    createMilestone(repository.owner, repository.name, form.title, form.description, form.dueDate)
    redirect("/%s/%s/issues/milestones".format(repository.owner, repository.name))
  })

  get("/:owner/:repository/issues/milestones/:milestoneId/edit")(collaboratorsOnly { repository =>
    issues.milestones.html.edit(getMilestone(repository.owner, repository.name, params("milestoneId").toInt), repository)
  })

  post("/:owner/:repository/issues/milestones/:milestoneId/edit", milestoneForm)(collaboratorsOnly { (form, repository) =>
    getMilestone(repository.owner, repository.name, params("milestoneId").toInt).map { milestone =>
      updateMilestone(milestone.copy(title = form.title, description = form.description, dueDate = form.dueDate))
      redirect("/%s/%s/issues/milestones".format(repository.owner, repository.repository))
    } getOrElse NotFound
  })

  get("/:owner/:repository/issues/milestones/:milestoneId/close")(collaboratorsOnly { repository =>
    getMilestone(repository.owner, repository.name, params("milestoneId").toInt).map { milestone =>
      closeMilestone(milestone)
      redirect("/%s/%s/issues/milestones".format(repository.owner, repository.name))
    } getOrElse NotFound
  })

  get("/:owner/:repository/issues/milestones/:milestoneId/open")(collaboratorsOnly { repository =>
    getMilestone(repository.owner, repository.name, params("milestoneId").toInt).map { milestone =>
      openMilestone(milestone)
      redirect("/%s/%s/issues/milestones".format(repository.owner, repository.name))
    } getOrElse NotFound
  })

  get("/:owner/:repository/issues/milestones/:milestoneId/delete")(collaboratorsOnly { repository =>
    getMilestone(repository.owner, repository.name, params("milestoneId").toInt).map { milestone =>
      deleteMilestone(repository.owner, repository.name, milestone.milestoneId)
      redirect("/%s/%s/issues/milestones".format(repository.owner, repository.name))
    } getOrElse NotFound
  })

}
