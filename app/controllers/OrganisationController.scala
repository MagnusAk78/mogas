package controllers

import javax.inject._

import org.joda.time.DateTime

import scala.concurrent.{ Await, Future, duration }, duration.Duration

import play.api.Logger

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

  def list(organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

    val selector = Organisation(allowedUsers = request.identity.uuid.toSet)

    Logger.info("OrganisationController.list , selector:" + selector)

    val responses = for {
      count <- organisationService.count(selector)
      orgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
    } yield {
      Logger.info("OrganisationController.list , count:" + count)
      Logger.info("OrganisationController.list , orgList:" + orgList)
      Logger.info("OrganisationController.list , request.identity.activeOrganisation:" + request.identity.activeOrganisation)
      Logger.info("OrganisationController.list , activeOrg:" + activeOrg)

      Ok(views.html.organisations.list(orgList, count, organisationsPage,
        utils.DefaultValues.DefaultPageLength, Some(request.identity), activeOrg))
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def submit = {
    def fs = Await.result(gridFS, Duration("5s"))

    silhouette.SecuredAction(AlwaysAuthorized()).async(gridFSBodyParser(fs)) { implicit request =>

      Logger.info("OrganisationController.submit")

      val optFutureFile = request.body.file(forms.imageFileKeyString) match {
        case Some(file) => file.ref.map(Some(_))
        case None       => Future.successful(None)
      }

      Logger.info("OrganisationController.submit _ 1")

      val responses = for {
        oldOrg <- request.body.asFormUrlEncoded.get("uuid") match {
          case Some(uuid :: ignoringTheTail) => organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
          case _                             => Future.successful(None)
        }
        fs <- gridFS
        optFile <- {
          Logger.info("OrganisationController.submit _ 2")

          optFutureFile
        }
        activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
        formResult <- OrganisationForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.organisations.edit(formWithErrors, true, Some(request.identity),
              activeOrg)))
          },
          formData => {

            Logger.info("OrganisationController.submit _ 3")

            //val imageFileReps = Organisation.saveImageFile(request.body.file(models.imageFileKeyString))

            oldOrg match {
              case Some(oo) => {
                Logger.info("OrganisationController.submit _ 4.1")

                val updateOrg = Organisation(
                  name = formData.name,
                  allowedUsers = oo.allowedUsers,
                  imageReadFileId = optFile.map(file => file.id.as[String]))

                val futureUpdateResult = organisationService.update(oo.uuid.get, updateOrg)

                futureUpdateResult map {
                  optNewOrg =>
                    optNewOrg.isDefined match {
                      case true => Redirect(routes.OrganisationController.edit(optNewOrg.map(_.uuid.get).get)).
                        flashing("success" -> Messages("db.success.update", updateOrg.name))
                      case false => Redirect(routes.OrganisationController.edit(formData.uuid.get)).
                        flashing("failure" -> Messages("db.success.update", updateOrg.name))
                    }
                }
              }
              case None => {
                Logger.info("OrganisationController.submit _ 4.2")
                val newOrg = Organisation.create(
                  name = formData.name,
                  allowedUsers = Set(request.identity.uuid.get),
                  imageReadFileId = optFile.map(file => file.id.as[String]))

                Logger.info("OrganisationController.submit _ 4.2.1")

                val futureSaveResult = organisationService.insert(newOrg)

                Logger.info("OrganisationController.submit _ 4.2.1.1 futureSaveResult:" + futureSaveResult)

                futureSaveResult map {
                  optNewOrg =>
                    optNewOrg.isDefined match {
                      case true =>
                        Logger.info("OrganisationController.submit _ 4.2.3")
                        Redirect(routes.OrganisationController.edit(optNewOrg.get.uuid.get)).
                          flashing("success" -> Messages("db.success.insert", newOrg.name))
                      case false =>
                        Logger.info("OrganisationController.submit _ 4.2.4")
                        Redirect(routes.OrganisationController.edit(formData.uuid.get)).
                          flashing("failure" -> Messages("db.success.insert", newOrg.name))
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
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
    } yield Ok(views.html.organisations.edit(OrganisationForm.form, false, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.edit")

    val responses = for {
      //Get the uuid from the form data and see if it exists in the database
      optOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
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

    val selector = Organisation(allowedUsers = request.identity.uuid.toSet)

    val responses = for {
      orgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      count <- organisationService.count(selector)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
    } yield Ok(views.html.organisations.editActivateOrganisation(orgList, count, organisationsPage,
      utils.DefaultValues.DefaultPageLength, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def setActiveOrganisation(uuid: String, organisationsPage: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

      val selector = Organisation(allowedUsers = request.identity.uuid.toSet)

      val responses = for {
        activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation), maxDocs = 1).map(_.headOption)
        updatedUser <- if (activeOrg.isDefined && activeOrg.get.uuid.get == uuid) {
          userService.update(request.identity.uuid.get, User(activeOrganisation = None))
        } else {
          userService.update(request.identity.uuid.get, User(activeOrganisation = Some(uuid)))
        }
        count <- organisationService.count(selector)
        orgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength) 
      } yield updatedUser match {
          case Some(updateUser) => Ok(views.html.organisations.editActivateOrganisation(orgList, count,
            organisationsPage, utils.DefaultValues.DefaultPageLength, updatedUser, activeOrg))
          case None => Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.update"))
        }

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editAllowedUsers(uuid: String, usersPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editAllowedUsers")

    val futureResponses = for {
      futureOpOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      userCount <- userService.count(User())
      futureUserList <- userService.find(User(), usersPage, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield (futureOpOrg, futureUserList, userCount, activeOrg)

    futureResponses.map(responses =>

      responses._1 match {
        case Some(organisation) => {
          Ok(views.html.organisations.editAllowedUsers(organisation, responses._2, responses._3, usersPage,
            utils.DefaultValues.DefaultPageLength, Some(request.identity), responses._4))
        }
        case None =>
          Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
      })
  }

  def changeAllowedUser(uuid: String, userIdString: String, usersPage: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("OrganisationController.changeAllowedUser")

      val futureResponses = for {
        opOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
        orgUpdate <- opOrg match {
          case Some(organisation) => {
            val newOrganisation = organisation.allowedUsers.contains(userIdString) match {
              case true  => organisation.copy(allowedUsers = organisation.allowedUsers - userIdString)
              case false => organisation.copy(allowedUsers = organisation.allowedUsers + userIdString)
            }

            if (newOrganisation.allowedUsers.isEmpty) {
              //This is not ok, some user must be allowed to see/change it
              Future.successful(Some(organisation), ("error" -> Messages("organisation.minimum.one.user")))
            } else if (newOrganisation.allowedUsers.contains(request.identity.uuid.get) == false) {
              //This is not ok, the logged in user must be part of the organisation.
              Future.successful(Some(organisation), ("error" -> Messages("organisation.remove.self.not.allowed")))
            } else {
              //Update the organisation
              organisationService.update(uuid, newOrganisation).map((_, "success" -> Messages("db.success.update", organisation.name)))
            }
          }
          case None => Future.successful(None, ("error" -> Messages("db.error.find")))
        }
      } yield (opOrg, orgUpdate)

      futureResponses.map(responses =>
        responses._1 match {
          case Some(organisation) => {
            if (organisation.allowedUsers.contains(request.identity.uuid.get)) {
              //Access allowed

              Redirect(routes.OrganisationController.editAllowedUsers(responses._2._1.get.uuid.get,
                usersPage)).flashing(responses._2._2)

            } else {
              Redirect(routes.OrganisationController.list(1)).
                flashing("error" -> Messages("access.denied"))
            }
          }
          case None =>
            val orgForm = OrganisationForm.form.fill(Organisation(uuid = Some(uuid)))
            Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
        })
    }

  def delete(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.delete uuid: " + uuid)

    val futureResponses = for {
      opOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      deletedOrg <- opOrg match {
        case Some(organisation) => {
          if (organisation.allowedUsers.contains(request.identity.uuid.get)) {
            //We can't remove organisations that have dependencies to factories
            //   val depFacCursor = FactoryDAO.find(FactoryParams(organisation = Some(organisation._id))).
            //     sort(DbHelper.sortAscKey(Factory.nameKey))

            // if (depFacCursor.isEmpty) {
            organisationService.remove(organisation).map((_, "success" -> Messages("db.success.remove", organisation.name)))
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
