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

import play.api.libs.iteratee.Iteratee

import com.sksamuel.scrimage._
import com.sksamuel.scrimage.nio.JpegWriter
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.JSONFileToSave



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
    val selector = Organisation.allowedUserQuery(request.identity.uuid)

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

  def create = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.create")

    val responses = for {
      activeOrg <- organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
    } yield Ok(views.html.organisations.edit(OrganisationForm.form, None, Some(request.identity), activeOrg))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def submitCreate = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationForm.form.bindFromRequest().fold(
      formWithErrors => {
        val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
        futureActiveOrg.map(activeOrg =>
          BadRequest(views.html.organisations.edit(formWithErrors, None, Some(request.identity), activeOrg)))
      },
      formData => {
        val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

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

    val futureOptOrg = organisationService.findOne(Organisation.uuidQuery(uuid))

    val futureOptActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

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
        val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))
        futureActiveOrg.map(activeOrg =>
          BadRequest(views.html.organisations.edit(formWithErrors, None, Some(request.identity), activeOrg)))
      },
      formData => {
        val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

        val futureOldOrg = organisationService.findOne(Organisation.uuidQuery(uuid))

        val responses = for {
          activeOrg <- futureActiveOrg
          oldOrg <- futureOldOrg
          updateResult <- {
            val updateOrg = OrganisationUpdate(name = Some(formData.name))

            organisationService.update(Organisation.uuidQuery(oldOrg.get.uuid), updateOrg.toSetJsObj)
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

  def submitImage(uuid: String) = {
    def fs = Await.result(gridFS, Duration("5s"))

    silhouette.SecuredAction(AlwaysAuthorized()).async(gridFSBodyParser(fs)) { implicit request =>

      Logger.info("OrganisationController: submitImage")

      val futureOldOrg = organisationService.findOne(Organisation.uuidQuery(uuid))

      val futureOptFileRef = request.body.file(forms.imageFileKeyString) match {
        case Some(file) => file.ref.map(Some(_))
        case None       => Future.successful(None)
      }

      //If the file is not an image or the correct size, just remove it
      val futureOptFile = futureOptFileRef.flatMap(optFileRef => optFileRef match {
        case Some(file) => {
          if (file.contentType.isEmpty ||
            file.contentType.get.startsWith("image") == false ||
            file.length < utils.DefaultValues.MinimumImageSize) {

            //File is not ok, remove it
            val result = fs.remove(file.id)

            //And return nothing
            Future.successful(None)
          } else {
            
            Logger.info("FILE UPLOADED CORRECTLY")
            
            val iterator = fs.enumerate(file).run(Iteratee.consume[Array[Byte]]())
            
            Logger.info("Iterator created")
            
            val resp = iterator.flatMap {
              bytes => {
                // Create resized image
                
                  val enumerator: Enumerator[Array[Byte]] = Enumerator.fromStream(Image(bytes).bound(120, 120).stream)

                  val data = JSONFileToSave(
                    filename = file.filename,
                    contentType = file.contentType,
                    uploadDate = Some(DateTime.now().getMillis))
                    
                    Logger.info("Saving the new image file")
                    val futureSaveResult = fs.save(enumerator, data)
                    
                    val totalResult = for {
                       saveResult <- futureSaveResult
                       removeResult <- {
                         Logger.info("Removing the old file")
                         fs.remove(file.id)
                       }
                       finalResult <- {
                         //This is only logging things, totally got to go or change
                         Logger.info("removeResult: " + removeResult)
                         Future.successful(removeResult)
                       }
                    } yield saveResult
                    
                    totalResult.map(Some(_))
                }
              }
            
            resp
            
            //File is ok, continue
            //Some(file)
          }
        }
        case None => Future.successful(None)
      })

      val futureActiveOrg = organisationService.findOne(Organisation.uuidQuery(request.identity.activeOrganisation))

      val responses = for {
        optFile <- futureOptFile
        oldOrg <- futureOldOrg
        activeOrg <- futureActiveOrg
        updateOrg <- Future.successful(OrganisationUpdate(imageReadFileId = Some(optFile.get.id.as[String])))
        remOldImageResult <- {
          //Before the organisation is updated the old image (if any) must be removed
          if(oldOrg.get.imageReadFileId != models.UuidNotSet) {
            fs.remove(JsString(oldOrg.get.imageReadFileId)).map(Some(_))
          } else {
            Future.successful(None)
          }
        }
        updateResult <- organisationService.update(Organisation.uuidQuery(oldOrg.get.uuid), updateOrg.toSetJsObj)
        result <- updateResult match {
          case true => Future.successful(Redirect(routes.OrganisationController.edit(uuid)).
            flashing("success" -> Messages("db.success.update", oldOrg.get.name)))
          case false => Future.successful(Redirect(routes.OrganisationController.edit(uuid)).
            flashing("failure" -> Messages("db.failed.update", oldOrg.get.name)))
        }
      } yield result

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }
  }

  def editActiveOrganisation(organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editActivatedOrganisation")

    val selector = Organisation.allowedUserQuery(request.identity.uuid)

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
      val selector = Organisation.allowedUserQuery(request.identity.uuid)

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
              case true  => OrganisationUpdate(allowedUsers = Some(organisation.allowedUsers - userUuid))
              case false => OrganisationUpdate(allowedUsers = Some(organisation.allowedUsers + userUuid))
            }

            if (newOrganisation.allowedUsers.isDefined && newOrganisation.allowedUsers.get.isEmpty) {
              //This is not ok, some user must be allowed to see/change it
              Future.successful(Some(organisation), ("error" -> Messages("organisation.minimum.one.user")))
            } else if (newOrganisation.allowedUsers.isDefined && 
                newOrganisation.allowedUsers.get.contains(request.identity.uuid) == false) {
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
