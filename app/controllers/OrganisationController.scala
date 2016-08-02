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
import models.daos.FileDAO
import models.services.FileService
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
import utils.PaginateData
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
import models.services.RemoveResult

@Singleton
class OrganisationController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val organisationService: OrganisationService,
  val userService: UserService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>
      val responses = for {
        organisationListData <- organisationService.getOrganisationList(page, mySecuredRequest.identity.uuid)
      } yield Ok(views.html.organisations.list(organisationListData.list, organisationListData.paginateData,
        Some(mySecuredRequest.identity), mySecuredRequest.activeOrganisation))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def create = generalActions.MySecuredAction { implicit mySecuredRequest =>
    Ok(views.html.organisations.create(OrganisationForm.form, Some(mySecuredRequest.identity),
      mySecuredRequest.activeOrganisation))
  }

  def submitCreate = generalActions.MySecuredAction.async { implicit mySecuredRequest =>
    OrganisationForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.organisations.create(formWithErrors, Some(mySecuredRequest.identity),
          mySecuredRequest.activeOrganisation)))
      },
      formData => {
        val responses = for {
          optSavedOrg <- organisationService.insert(Organisation.
            create(name = formData.name, allowedUsers = Set(mySecuredRequest.identity.uuid)))
        } yield optSavedOrg match {
          case Some(newOrg) =>
            Redirect(routes.OrganisationController.edit(newOrg.uuid)).
              flashing("success" -> Messages("db.success.insert", newOrg.name))
          case None =>
            Redirect(routes.OrganisationController.create).
              flashing("failure" -> Messages("db.failure.insert", formData.name))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      })
  }

  def edit(uuid: String) =
    (generalActions.MySecuredAction andThen
      generalActions.OrganisationAction(uuid)) { implicit myOrganisationRequest =>

        Ok(views.html.organisations.edit(myOrganisationRequest.organisation,
          OrganisationForm.form.fill(myOrganisationRequest.organisation), Some(myOrganisationRequest.identity),
          myOrganisationRequest.activeOrganisation))
      }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.OrganisationAction(uuid)).async { implicit myOrganisationRequest =>
      OrganisationForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.organisations.edit(myOrganisationRequest.organisation, formWithErrors,
            Some(myOrganisationRequest.identity), myOrganisationRequest.activeOrganisation)))
        },
        formData => {
          val responses = for {
            updateResult <- {
              val updateOrg = myOrganisationRequest.organisation.copy(name = formData.name)
              organisationService.update(updateOrg)
            }
          } yield updateResult match {
            case true =>
              Redirect(routes.OrganisationController.edit(uuid)).
                flashing("success" -> Messages("db.success.update", formData.name))
            case false =>
              Redirect(routes.OrganisationController.edit(uuid)).
                flashing("failure" -> Messages("db.failure.update", formData.name))
          }

          responses recover {
            case e => InternalServerError(e.getMessage())
          }
        })
    }

  def editActiveOrganisation(page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>
      val responses = for {
        organisationListData <- organisationService.getOrganisationList(page, mySecuredRequest.identity.uuid)
      } yield Ok(views.html.organisations.editActivateOrganisation(organisationListData.list, organisationListData.paginateData,
        Some(mySecuredRequest.identity), mySecuredRequest.activeOrganisation))
      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def setActiveOrganisation(uuid: String, page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>

      val responses = for {
        organisationListData <- organisationService.getOrganisationList(page, mySecuredRequest.identity.uuid)
        updateUserResult <- {
          val updateUser = if (mySecuredRequest.activeOrganisation.isDefined && mySecuredRequest.activeOrganisation.get.uuid == uuid) {
            mySecuredRequest.identity.copy(activeOrganisation = models.UuidNotSet)
          } else {
            mySecuredRequest.identity.copy(activeOrganisation = uuid)
          }
          userService.update(updateUser)
        }
        optNewLoggedInUser <- userService.findOneByUuid(mySecuredRequest.identity.uuid)
        otpNewActiveOrg <- optNewLoggedInUser match {
          case Some(newLoggedInUser) => organisationService.findOneByUuid(newLoggedInUser.activeOrganisation)
          case None => Future.failed(new Exception("No new user found after update"))
        }
      } yield Ok(views.html.organisations.editActivateOrganisation(organisationListData.list,
        organisationListData.paginateData, optNewLoggedInUser, otpNewActiveOrg))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editAllowedUsers(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.OrganisationAction(uuid)).async { implicit organisationRequest =>
      val responses = for {
        userListData <- userService.getUserList(page, organisationRequest.organisation.allowedUsers)
      } yield Ok(views.html.organisations.editAllowedUsers(organisationRequest.organisation, userListData.list,
        userListData.paginateData, Some(organisationRequest.identity), organisationRequest.activeOrganisation))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def changeAllowedUser(uuid: String, userUuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.OrganisationAction(uuid)).async { implicit organisationRequest =>

      (organisationRequest.organisation.allowedUsers.contains(organisationRequest.identity.uuid))

      val newOrganisation = organisationRequest.organisation.copy(allowedUsers = organisationRequest.organisation.allowedUsers.contains(userUuid) match {
        case true => organisationRequest.organisation.allowedUsers - userUuid
        case false => organisationRequest.organisation.allowedUsers + userUuid
      })

      if (newOrganisation.allowedUsers.isEmpty) {
        //This is not ok, some user must be allowed to see/change it
        Future.successful(Redirect(routes.OrganisationController.editAllowedUsers(uuid, page)).
          flashing("error" -> Messages("thereMustBeAtLeastOneAllowedUser")))
      } else if (newOrganisation.allowedUsers.contains(organisationRequest.identity.uuid) == false) {
        //This is not ok, the logged in user must be part of the organisation.
        Future.successful(Redirect(routes.OrganisationController.editAllowedUsers(uuid, page)).
          flashing("error" -> Messages("accessDenied")))
      } else {
        val responses = for {
          //Update the organisation
          updateSuccess <- organisationService.update(newOrganisation)
        } yield if (updateSuccess) {
          Redirect(routes.OrganisationController.editAllowedUsers(newOrganisation.uuid, page)).
            flashing("success" -> Messages("db.success.update", organisationRequest.organisation.name))
        } else {
          Redirect(routes.OrganisationController.editAllowedUsers(newOrganisation.uuid, page)).
            flashing("error" -> Messages("db.failure.update"))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }
    }

  def delete(uuid: String) = (generalActions.MySecuredAction andThen generalActions.OrganisationAction(uuid)).async { implicit organisationRequest =>
    val responses = for {
      removeResult <- organisationService.remove(organisationRequest.organisation, organisationRequest.identity.uuid)
    } yield if (removeResult.success) {
      Redirect(routes.OrganisationController.list(1)).flashing("success" -> Messages("db.success.remove", organisationRequest.organisation.name))
    } else {
      Redirect(routes.OrganisationController.list(1)).flashing("error" -> removeResult.getReason)
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }
}
