package controllers

import javax.inject._

import org.joda.time.DateTime

import scala.concurrent.{ Await, Future, duration }, duration.Duration

import play.api.Logger

import models.OrganisationUpdate
import models.services.OrganisationService
import models.services.UserService

import forms.OrganisationForm

import play.api.mvc.{ Action, Controller, Request }
import play.api.libs.json.{ Json, JsObject, JsString }

import reactivemongo.api.gridfs.{ GridFS, ReadFile }

import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import models.Organisation
import models.User
import akka.stream.Materializer
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.i18n.Messages
import scala.concurrent.ExecutionContext
import com.mohiva.play.silhouette.api.Silhouette
import utils.auth.DefaultEnv
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.UserUpdate

@Singleton
class OrganisationController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv],
  val organisationService: OrganisationService,
  val userService: UserService,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  import MongoController.readFileReads

  //private val gridFS = reactiveMongoApi.gridFS
  private val gridFS = for {
    fs <- reactiveMongoApi.database.map(db =>
      GridFS[JSONSerializationPack.type](db))
    _ <- fs.ensureIndex().map { index =>
      // let's build an index on our gridfs chunks collection if none
      Logger.info(s"Checked index, result is $index")
    }
  } yield fs

  def list(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    
    Logger.info("OrganisationController.list organisationsPage:" + page)

    //Non-future values
    val selector = Organisation.allowedUsersQuery(Set(request.identity.uuid))
    
    //Future values that can start in parallel
    val futureOrgCount = organisationService.count(selector)
    val futureOrgList = organisationService.find(selector, page, utils.DefaultValues.DefaultPageLength)
    val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

    val responses = for {
      count <- futureOrgCount
      orgList <- futureOrgList
      activeOrg <- futureActiveOrg
    } yield Ok(views.html.organisations.list(orgList, count, page,
        utils.DefaultValues.DefaultPageLength, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def submit = {
    def fs = Await.result(gridFS, Duration("5s"))

    silhouette.SecuredAction(AlwaysAuthorized()).async(gridFSBodyParser(fs)) { implicit request =>

      val responses = for {
        optFile <- request.body.file(forms.imageFileKeyString) match {
          case Some(file) => file.ref.map(Some(_))
          case None       => Future.successful(None)
        }
        oldOrg <- request.body.asFormUrlEncoded.get("uuid") match {
          case Some(uuid :: ignoringTheTail) => organisationService.findOne(Organisation.uuidQuery(uuid))
          case _                             => Future.successful(None)
        }
        //fs <- gridFS
        activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
        formResult <- OrganisationForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.organisations.edit(formWithErrors, true, Some(request.identity),
              activeOrg)))
          },
          formData => {

            Logger.info("OrganisationController.submit _ 3")

            oldOrg match {
              case Some(oo) => {
                Logger.info("OrganisationController.submit _ 4.1")

                val updateOrg = OrganisationUpdate(
                  name = Some(formData.name),
                  allowedUsers = oo.allowedUsers,
                  imageReadFileId = optFile.map(file => file.id.as[String]))

                val futureUpdateResult = organisationService.update(Organisation.uuidQuery(oo.uuid), updateOrg.toSetJsObj)

                futureUpdateResult map {
                  optNewOrg =>
                    optNewOrg match {
                      case true => Redirect(routes.OrganisationController.edit(oo.uuid)).
                        flashing("success" -> Messages("db.success.update", formData.name))
                      case false => Redirect(routes.OrganisationController.edit(oo.uuid)).
                        flashing("failure" -> Messages("db.failed.update", oo.name))
                    }
                }
              }
              case None => {
                val newOrg = Organisation.create(
                  name = formData.name,
                  allowedUsers = Set(request.identity.uuid),
                  imageReadFileId = optFile match {
                    case Some(file) => file.id.as[String]
                    case None       => models.UuidNotSet
                  })

                val futureSaveResult = organisationService.insert(newOrg)

                futureSaveResult map {
                  optNewOrg =>
                    optNewOrg.isDefined match {
                      case true =>
                        Redirect(routes.OrganisationController.edit(optNewOrg.get.uuid)).
                          flashing("success" -> Messages("db.success.insert", newOrg.name))
                      case false =>
                        Redirect(routes.OrganisationController.create).flashing("failure" -> Messages("db.failure.insert", formData.name))
                    }
                }
              }

            }
          })
      } yield formResult

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }
  }

  def create = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.create")

    val responses = for {
      activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
    } yield Ok(views.html.organisations.edit(OrganisationForm.form, false, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.edit")

    val responses = for {
      //Get the uuid from the form data and see if it exists in the database
      optOrg <- organisationService.findOne(Organisation.uuidQuery(uuid))
      activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
    } yield optOrg match {
      case Some(organisation) => {
        Ok(views.html.organisations.edit(OrganisationForm.form.fill(organisation), true, Some(request.identity), activeOrg))
      }
      case None =>
        Ok(views.html.organisations.edit(OrganisationForm.form, false, Some(request.identity), activeOrg))
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def editActiveOrganisation(organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editActivatedOrganisation")

    val selector = Organisation.allowedUsersQuery(Set(request.identity.uuid))

    val responses = for {
      orgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      count <- organisationService.count(selector)
      activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
    } yield Ok(views.html.organisations.editActivateOrganisation(orgList, count, organisationsPage,
      utils.DefaultValues.DefaultPageLength, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def setActiveOrganisation(uuid: String, organisationsPage: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

      //Non-future values
      val selector = Organisation.allowedUsersQuery(Set(request.identity.uuid))

      //Future values for parallel execution
      val futureOrgCount = organisationService.count(selector)
      val futureOrgList = organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      val futurePrevActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

      //Sequential evaluation with for comprehension 
      val responses = for {
        orgCount <- futureOrgCount
        orgList <- futureOrgList
        prevActiveOrg <- futurePrevActiveOrg
        updateUserResult <- {
          if (prevActiveOrg.isDefined && prevActiveOrg.get.uuid == uuid) {
            userService.update(User.uuidQuery(request.identity.uuid),
              UserUpdate(activeOrganisation = Some(models.UuidNotSet)).toSetJsObj)
          } else {
            userService.update(User.uuidQuery(request.identity.uuid),
              UserUpdate(activeOrganisation = Some(uuid)).toSetJsObj)
          }
        }
        optNewLoggedInUser <- userService.findOne(User.uuidQuery(request.identity.uuid))
        otpNewActiveOrg <- optNewLoggedInUser match {
          case Some(newLoggedInUser) => organisationService.findOne(Organisation.uuidQuery(newLoggedInUser.activeOrganisation))
          case None                  => Future.failed(new Exception("No new user found after update"))
        }
      } yield Ok(views.html.organisations.editActivateOrganisation(orgList, orgCount,
        organisationsPage, utils.DefaultValues.DefaultPageLength, optNewLoggedInUser, otpNewActiveOrg))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editAllowedUsers(uuid: String, page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editAllowedUsers")

    val futureResponses = for {
      futureOpOrg <- organisationService.findOne(Organisation.uuidQuery(uuid))
      userCount <- userService.count(Json.obj())
      futureUserList <- userService.find(Json.obj(), page, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
    } yield (futureOpOrg, futureUserList, userCount, activeOrg)

    futureResponses.map(responses =>

      responses._1 match {
        case Some(organisation) => {
          Ok(views.html.organisations.editAllowedUsers(organisation, responses._2, responses._3, page,
            utils.DefaultValues.DefaultPageLength, Some(request.identity), responses._4))
        }
        case None =>
          Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
      })
  }

  def changeAllowedUser(uuid: String, userUuid: String, page: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("OrganisationController.changeAllowedUser")

      val responses = for {
        opOrg <- organisationService.findOne(Organisation.uuidQuery(uuid))
        orgUpdate <- opOrg match {
          case Some(organisation) => {
            val newOrganisation = organisation.allowedUsers.contains(userUuid) match {
              case true  => OrganisationUpdate(allowedUsers = organisation.allowedUsers - userUuid)
              case false => OrganisationUpdate(allowedUsers = organisation.allowedUsers + userUuid)
            }

            if (newOrganisation.allowedUsers.isEmpty) {
              //This is not ok, some user must be allowed to see/change it
              Future.successful(Some(organisation), ("error" -> Messages("organisation.minimum.one.user")))
            } else if (newOrganisation.allowedUsers.contains(request.identity.uuid) == false) {
              //This is not ok, the logged in user must be part of the organisation.
              Future.successful(Some(organisation), ("error" -> Messages("organisation.remove.self.not.allowed")))
            } else {

              //Update the organisation
              organisationService.update(Organisation.uuidQuery(uuid), newOrganisation.toSetJsObj).
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

    val futureResponses = for {
      opOrg <- organisationService.findOne(Organisation.uuidQuery(uuid))
      deletedOrg <- opOrg match {
        case Some(organisation) => {
          if (organisation.allowedUsers.contains(request.identity.uuid)) {
            //We can't remove organisations that have dependencies to factories
            //   val depFacCursor = FactoryDAO.find(FactoryParams(organisation = Some(organisation._id))).
            //     sort(DbHelper.sortAscKey(Factory.nameKey))

            // if (depFacCursor.isEmpty) {
            organisationService.remove(Organisation.uuidQuery(organisation.uuid)).map((_, "success" -> Messages("db.success.remove", organisation.name)))
            /* } else {
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.dependencies", "Factory: " + depFacCursor.next.name)))
          }*/
          } else {
            Future.successful(None, ("error" -> Messages("access.denied")))
          }
        }
        case None => Future.successful(None, ("error" -> Messages("db.error.find", "Organisation", uuid)))
      }
    } yield (opOrg, deletedOrg)

    futureResponses.map(responses =>
      Redirect(routes.OrganisationController.list(1)).flashing(responses._2._2))
  }
}
