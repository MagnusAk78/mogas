package controllers

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import com.mohiva.play.silhouette.api.Silhouette
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.writer

import akka.stream.Materializer
import forms.OrganisationForm
import forms.OrganisationForm.fromOrganisationToData
import javax.inject.Inject
import javax.inject.Singleton
import models.Organisation
import models.daos.FileDAO
import models.services.FileService
import models.services.OrganisationService
import models.services.UserService
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import play.modules.reactivemongo.JSONFileToSave
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.ReactiveMongoComponents
import reactivemongo.play.json.JsFieldBSONElementProducer
import utils.PaginateData
import utils.auth.DefaultEnv
import models.Images

@Singleton
class OrganisationController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv],
  val organisationService: OrganisationService,
  val userService: UserService,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  def list(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

    Logger.info("OrganisationController.list organisationsPage:" + page)

    //Non-future values
    val selector = Organisation.allowedUserQuery(request.identity.uuid)

    //Future values that can start in parallel
    val futureOrgCount = organisationService.count(selector)
    val futureOrgList = organisationService.find(selector, page, utils.DefaultValues.DefaultPageLength)
    val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

    val responses = for {
      count <- futureOrgCount
      activeOrg <- futureActiveOrg
      orgList <- futureOrgList
    } yield Ok(views.html.organisations.list(orgList, PaginateData(page, count),
      Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def create = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.create")

    val responses = for {
      activeOrg <- organisationService.findOneByUuid(request.identity.activeOrganisation)
    } yield Ok(views.html.organisations.edit(OrganisationForm.form, None, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def submitCreate = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationForm.form.bindFromRequest().fold(
      formWithErrors => {
        val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)
        futureActiveOrg.map(activeOrg =>
          BadRequest(views.html.organisations.edit(formWithErrors, None, Some(request.identity), activeOrg)))
      },
      formData => {
        val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

        val newOrg = Organisation.create(name = formData.name, allowedUsers = Set(request.identity.uuid))

        val futureOptSavedOrg = organisationService.insert(newOrg)

        futureOptSavedOrg map { optSavedOrg =>
          optSavedOrg.isDefined match {
            case true =>
              Redirect(routes.OrganisationController.edit(newOrg.uuid)).
                flashing("success" -> Messages("db.success.insert", newOrg.name))
            case false =>
              Redirect(routes.OrganisationController.create).
                flashing("failure" -> Messages("db.failure.insert", formData.name))
          }
        }
      })
  }

  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.edit")

    val futureOptOrg = organisationService.findOneByUuid(uuid)

    val futureOptActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

    Logger.info("OrganisationController.edit 2")

    val responses = for {
      activeOrg <- futureOptActiveOrg
      optOrg <- futureOptOrg
    } yield optOrg match {
      case Some(organisation) => {
        Logger.info("OrganisationController.edit organisation:" + organisation)
        Ok(views.html.organisations.edit(OrganisationForm.form.fill(organisation), Some(uuid), Some(request.identity), activeOrg))
      }
      case None =>
        Logger.info("OrganisationController.edit None:")
        Ok(views.html.organisations.edit(OrganisationForm.form, Some(uuid), Some(request.identity), activeOrg))
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def submitEdit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationForm.form.bindFromRequest().fold(
      formWithErrors => {
        val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)
        futureActiveOrg.map(activeOrg =>
          BadRequest(views.html.organisations.edit(formWithErrors, None, Some(request.identity), activeOrg)))
      },
      formData => {
        val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

        val futureOldOrg = organisationService.findOneByUuid(uuid)

        val responses = for {
          activeOrg <- futureActiveOrg
          oldOrg <- futureOldOrg
          updateResult <- {
            val updateOrg = oldOrg.get.copy(name = formData.name)
            organisationService.update(updateOrg.uuidQuery, updateOrg.updateQuery)
          }
        } yield updateResult match {
          case true =>
            Redirect(routes.OrganisationController.list(1)).
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

  def editActiveOrganisation(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editActivatedOrganisation")

    val selector = Organisation.allowedUserQuery(request.identity.uuid)

    val responses = for {
      orgList <- organisationService.find(selector, page, utils.DefaultValues.DefaultPageLength)
      count <- organisationService.count(selector)
      activeOrg <- organisationService.findOneByUuid(request.identity.activeOrganisation)
    } yield Ok(views.html.organisations.editActivateOrganisation(orgList, PaginateData(page, count),
      Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def setActiveOrganisation(uuid: String, organisationsPage: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

      //Non-future values
      val selector = Organisation.allowedUserQuery(request.identity.uuid)

      //Future values for parallel execution
      val futureOrgCount = organisationService.count(selector)
      val futureOrgList = organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      val futurePrevActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

      //Sequential evaluation with for comprehension 
      val responses = for {
        orgCount <- futureOrgCount
        orgList <- futureOrgList
        prevActiveOrg <- futurePrevActiveOrg
        updateUserResult <- {
          val updateUser = if (prevActiveOrg.isDefined && prevActiveOrg.get.uuid == uuid) {
            request.identity.copy(activeOrganisation = models.UuidNotSet)
          } else {
            request.identity.copy(activeOrganisation = uuid)
          }
          userService.update(updateUser.uuidQuery, updateUser.updateQuery)
        }
        optNewLoggedInUser <- userService.findOneByUuid(request.identity.uuid)
        otpNewActiveOrg <- optNewLoggedInUser match {
          case Some(newLoggedInUser) => organisationService.findOneByUuid(newLoggedInUser.activeOrganisation)
          case None                  => Future.failed(new Exception("No new user found after update"))
        }
      } yield Ok(views.html.organisations.editActivateOrganisation(orgList, PaginateData(organisationsPage, orgCount),
        optNewLoggedInUser, otpNewActiveOrg))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editAllowedUsers(uuid: String, page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editAllowedUsers")

    val responses = for {
      opOrg <- organisationService.findOneByUuid(uuid)
      userCount <- userService.count(Json.obj())
      userList <- userService.find(Json.obj(), page, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.findOneByUuid(request.identity.activeOrganisation)
    } yield opOrg match {
      case Some(organisation) => Ok(views.html.organisations.editAllowedUsers(organisation, userList,
        PaginateData(page, userCount), Some(request.identity), activeOrg))
      case None => Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def changeAllowedUser(uuid: String, userUuid: String, page: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("OrganisationController.changeAllowedUser")

      val responses = for {
        opOrg <- organisationService.findOneByUuid(uuid)
        orgUpdate <- opOrg match {
          case Some(organisation) => {
            val newOrganisation = organisation.allowedUsers.contains(userUuid) match {
              case true  => organisation.copy(allowedUsers = organisation.allowedUsers - userUuid)
              case false => organisation.copy(allowedUsers = organisation.allowedUsers + userUuid)
            }

            if (newOrganisation.allowedUsers.isEmpty) {
              //This is not ok, some user must be allowed to see/change it
              Future.successful(Some(organisation), ("error" -> Messages("organisation.minimum.one.user")))
            } else if (newOrganisation.allowedUsers.contains(request.identity.uuid) == false) {
              //This is not ok, the logged in user must be part of the organisation.
              Future.successful(Some(organisation), ("error" -> Messages("organisation.remove.self.not.allowed")))
            } else {
              //Update the organisation
              organisationService.update(newOrganisation.uuidQuery, newOrganisation.updateQuery).
                map(a => (Some(organisation), ("success" -> Messages("db.success.update", organisation.name))))
            }
          }
          case None => Future.successful(None, ("error" -> Messages("db.error.find")))
        }
      } yield opOrg match {
        case Some(organisation) => {
          if (organisation.allowedUsers.contains(request.identity.uuid)) {
            //Access allowed

            Redirect(routes.OrganisationController.editAllowedUsers(orgUpdate._1.get.uuid, page)).flashing(orgUpdate._2)

          } else {
            Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("access.denied"))
          }
        }
        case None =>
          Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
      }

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def delete(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.delete uuid: " + uuid)

    val responses = for {
      opOrg <- organisationService.findOneByUuid(uuid)
      results <- opOrg match {
        case Some(organisation) => {
          //Check that it can be deleted
          if (organisation.allowedUsers.contains(request.identity.uuid)) {
            //We can't remove organisations that have dependencies to factories
            //   val depFacCursor = FactoryDAO.find(FactoryParams(organisation = Some(organisation._id))).
            //     sort(DbHelper.sortAscKey(Factory.nameKey))

            // if (depFacCursor.isEmpty) {
            val removeResult = organisationService.remove(organisation)

            val res = removeResult.map(rs => rs.success match {
              case true =>
                Redirect(routes.OrganisationController.list(1)).
                  flashing("success" -> Messages("db.success.remove", organisation.name))
              case false =>
                Redirect(routes.OrganisationController.list(1)).
                  flashing("error" -> rs.reason.getOrElse("NO REASON!!!"))
            })

            res
          } else {
            Future.successful(Redirect(routes.OrganisationController.edit(uuid)).flashing("error" -> Messages("access.denied")))
          }
        }
        case None => Future.successful(Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.find", "Organisation", uuid)))
      }
    } yield results

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }
}
