package controllers

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import akka.stream.Materializer
import models.formdata.OrganisationForm
import models.formdata.OrganisationForm.fromOrganisationToData
import javax.inject.Inject
import javax.inject.Singleton
import models.Organisation
import models.services.FactoryService
import models.services.OrganisationService
import models.services.UserService
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import utils.auth.DefaultEnv
import models.Images
import play.api.mvc.WrappedRequest
import play.api.mvc.Request
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc.ActionRefiner
import play.api.mvc.ActionTransformer
import play.api.mvc.ActionBuilder
import play.api.mvc.Flash
import controllers.actions._
import utils.RemoveResult
import models.formdata.FactoryForm
import models.Factory
import models.services.IssueService
import models.formdata.IssueForm
import models.Issue
import models.services.AmlObjectService
import models.formdata.IssueUpdateForm
import models.IssueUpdate
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import models.AmlObject

@Singleton
class IssueController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val userService: UserService,
  val organisationService: OrganisationService,
  val amlObjectService: AmlObjectService,
  val factoryService: FactoryService,
  val issueService: IssueService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(factoryUuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation).async {
      implicit factoryRequest =>

        val responses = for {
          factoryOpt <- {
            if (factoryUuid.isEmpty()) {
              Future.successful(None)
            } else {
              factoryService.findOneFactory(Factory.queryByUuid(factoryUuid))
            }
          }
          issueListData <- issueService.getIssueList(page)
          objectChainList <- factoryService.getAmlObjectChains(issueListData.list)
        } yield Ok(views.html.issues.list(factoryOpt, issueListData.list, objectChainList,
          issueListData.paginateData, Some(factoryRequest.identity), factoryRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def create(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation) {
      implicit mySecuredRequest =>
        Ok(views.html.issues.create(IssueForm.form, amlObjectUuid, Some(mySecuredRequest.identity),
          mySecuredRequest.activeOrganisation))
    }

  def submitCreate(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.AmlObjectAction(amlObjectUuid)).async {
      implicit amlObjectRequest =>
        IssueForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.issues.create(formWithErrors, amlObjectUuid,
              Some(amlObjectRequest.identity), amlObjectRequest.activeOrganisation)))
          },
          formData => {
            val responses = for {
              optSavedIssue <- issueService.insertIssue(Issue.create(name = formData.name,
                connectionToToFactory = amlObjectRequest.elementOrInterface.fold(_.connectionTo, _.connectionTo),
                parentAmlObject = amlObjectRequest.elementOrInterface.fold(_.uuid, _.uuid),
                createdBy = amlObjectRequest.identity.uuid))
            } yield optSavedIssue match {
              case Some(newIssue) =>
                //TODO: Change to edit when exist
                Redirect(routes.IssueController.list("", 1)).
                  flashing("success" -> Messages("db.success.insert", newIssue.name))
              case None =>
                Redirect(routes.IssueController.create(amlObjectUuid)).
                  flashing("failure" -> Messages("db.failure.insert", formData.name))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })
    }

  def issue(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueAction(uuid)).async {
      implicit issueRequest =>

        val responses = for {
          factoryOpt <- factoryService.findOneFactory(Factory.queryByUuid(issueRequest.issue.connectionTo))
          amlObjectOpt <- amlObjectService.findOneElementOrInterface(AmlObject.queryByUuid(issueRequest.issue.parent))
          issueUpdatesListData <- issueService.getIssueUpdateList(issueRequest.issue, page)
        } yield Ok(views.html.issues.issue(issueRequest.issue, amlObjectOpt.get, factoryOpt.get,
          issueUpdatesListData.list, issueUpdatesListData.paginateData, Some(issueRequest.identity),
          issueRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def inspectIssueUpdate(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueUpdateAction(uuid)).async {
      implicit issueUpdateRequest =>

        val responses = for {
          issueOpt <- issueService.findOneIssue(Issue.queryByUuid(issueUpdateRequest.issueUpdate.parent))
        } yield issueOpt.map(issue => Ok(views.html.issues.inspectIssueUpdate(
          issueUpdateRequest.issueUpdate, issue, page,
          Some(issueUpdateRequest.identity), issueUpdateRequest.activeOrganisation))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }

    }

  def submitCreateIssueUpdate(issueUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueAction(issueUuid)).async {
      implicit issueRequest =>

        IssueUpdateForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.issues.createIssueUpdate(issueRequest.issue,
              formWithErrors, Some(issueRequest.identity), issueRequest.activeOrganisation)))
          },
          formData => {
            val responses = for {
              nextOrderNumber <- issueService.getNextOrderNumber(issueRequest.issue)
              optSavedIssueUpdate <- issueService.insertIssueUpdate(IssueUpdate.create(
                orderNumber = nextOrderNumber,
                parentIssue = issueUuid,
                text = formData.text,
                createdBy = issueRequest.identity.uuid,
                created = System.currentTimeMillis(), closed = false, priority = 1))
            } yield optSavedIssueUpdate match {
              case Some(newIssueUpdate) =>
                //TODO: Change to edit when exist
                Redirect(routes.IssueController.issue(issueUuid, 1)).
                  flashing("success" -> Messages("db.success.insert", newIssueUpdate.uuid))
              case None =>
                Redirect(routes.IssueController.createIssueUpdate(issueUuid)).
                  flashing("failure" -> Messages("db.failure.insert"))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })
    }

  def createIssueUpdate(issueUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueAction(issueUuid)) {
      implicit issueRequest =>

        Ok(views.html.issues.createIssueUpdate(issueRequest.issue, IssueUpdateForm.form,
          Some(issueRequest.identity), issueRequest.activeOrganisation))
    }

  def submitEditIssueUpdate(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueUpdateAction(uuid)).async {
      implicit issueUpdateRequest =>

        IssueUpdateForm.form.bindFromRequest().fold(
          formWithErrors => {

            val responses = for {
              issueOpt <- issueService.findOneIssue(Issue.queryByUuid(issueUpdateRequest.issueUpdate.parent))
            } yield issueOpt.map(issue => Ok(views.html.issues.editIssueUpdate(issue,
              issueUpdateRequest.issueUpdate,
              formWithErrors, Some(issueUpdateRequest.identity),
              issueUpdateRequest.activeOrganisation))).getOrElse(NotFound)

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          },
          formData => {
            val responses = for {
              updateResult <- {
                val updateissueUpdate = issueUpdateRequest.issueUpdate.copy(text = formData.text)
                issueService.updateIssueUpdate(updateissueUpdate)
              }
            } yield updateResult match {
              case true =>
                Redirect(routes.IssueController.editIssueUpdate(uuid)).
                  flashing("success" -> Messages("db.success.update"))
              case false =>
                Redirect(routes.IssueController.editIssueUpdate(uuid)).
                  flashing("failure" -> Messages("db.failure.update"))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })

    }

  def editIssueUpdate(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.IssueUpdateAction(uuid)).async {
      implicit issueUpdateRequest =>

        val responses = for {
          issueOpt <- issueService.findOneIssue(Issue.queryByUuid(issueUpdateRequest.issueUpdate.parent))
        } yield issueOpt.map(issue => Ok(views.html.issues.editIssueUpdate(issue,
          issueUpdateRequest.issueUpdate,
          IssueUpdateForm.form.fill(issueUpdateRequest.issueUpdate), Some(issueUpdateRequest.identity),
          issueUpdateRequest.activeOrganisation))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }
}