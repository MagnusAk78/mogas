package controllers

import javax.inject.Inject
import javax.inject.Singleton

import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models._
import play.Logger
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.Controller

import scala.concurrent.Future
import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import com.sun.corba.se.spi.ior.ObjectId
import utils.auth.DefaultEnv
import models.services.UserService
import models.services.OrganisationService
import forms.OrganisationForm
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.ReactiveMongoComponents
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.gridfs.ReadFile
import play.api.libs.json.JsString
import reactivemongo.play.json.JSONSerializationPack
import akka.stream.Materializer
import play.api.libs.json._

import reactivemongo.api.gridfs.{ GridFS, ReadFile }

import play.modules.reactivemongo.json._, ImplicitBSONHandlers._
import play.modules.reactivemongo.json.collection._
import scala.concurrent.Await

@Singleton
class OrganisationController @Inject() (
    val messagesApi: MessagesApi, 
    val silhouette: Silhouette[DefaultEnv],
    val organisationService: OrganisationService, 
    val userService: UserService,
    val reactiveMongoApi: ReactiveMongoApi,
    implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {
  
  import MongoController._
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]
  
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

    val responses = for {
      futureCount <- organisationService.count(selector)
      futureOrgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield (futureOrgList, futureCount, activeOrg)

    responses.map(results =>
      Ok(views.html.organisations.list(results._1, results._2, organisationsPage, utils.DefaultValues.DefaultPageLength,
        Some(request.identity), results._3)))
  }

  def submit = {
    def fs = Await.result(gridFS, Duration("5s"))
    silhouette.SecuredAction(AlwaysAuthorized()).async(gridFSBodyParser(fs)) { implicit request =>
        
    Logger.info("OrganisationController.submit")
    
    val futureFile = request.body.files.head.ref
    
    futureFile.onFailure {
        case err => err.printStackTrace()
      }

    val responses = for {
      oldOrg <- request.body.asFormUrlEncoded.get("uuid") match {
        case Some(uuid :: ignoringTheTail) => organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
        case _ => Future.successful(None)
      }
      fs <- gridFS
      file <- { 
        Logger.info("_0")
        futureFile 
        }
        // here, the file is completely uploaded, so it is time to update the article
          updateResult <- fs.files.update(
            Json.obj("_id" -> file.id),
            Json.obj("$set" -> Json.obj("article" -> oldOrg.get.uuid)))
          }
      
      //Get the uuid from the form data and see if it exists in the database

      activeOrg <- request.identity.activeOrganisation.isDefined match {
        case true  => organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
        case false => Future.successful(None)
      }
    } yield {

      OrganisationForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.organisations.edit(formWithErrors, true, Some(request.identity),
            activeOrg)))
        },
        formData => {

          //val imageFileReps = Organisation.saveImageFile(request.body.file(models.imageFileKeyString))

          oldOrg match {
            case Some(oo) => {
              val updateOrg = Organisation(
              name = formData.name,
              allowedUsers = oo.allowedUsers /*,
          		imageFileRep = imageFileReps._1.orElse(organisationOpt.flatMap(_.imageFileRep)),
          		thumbnailFileRep = imageFileReps._2.orElse(organisationOpt.flatMap(_.thumbnailFileRep)) */ ) 
          		
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
              val newOrg = Organisation.create(formData.name, 
                allowedUsers = Set(request.identity.uuid.get))
                
                val futureSaveResult = organisationService.insert(newOrg)
                
          futureSaveResult map {
            optNewOrg =>
              optNewOrg.isDefined match {
                case true => Redirect(routes.OrganisationController.edit(optNewOrg.map(_.uuid.get).get)).
                  flashing("success" -> Messages("db.success.insert", newOrg.name))
                case false => Redirect(routes.OrganisationController.edit(formData.uuid.get)).
                  flashing("failure" -> Messages("db.success.insert", newOrg.name))
              }
          }                
              }
              
          }
        })
    }
  }
  
  def create = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.create")

    val futureResponses = for {
      //Get the uuid from the form data and see if it exists in the database
      futureActiveOrg <- request.identity.activeOrganisation.isDefined match {
        case true  => organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
        case false => Future.successful(None)
      }
    } yield futureActiveOrg

    futureResponses.map( activeOrgOpt => 
      Ok(views.html.organisations.edit(OrganisationForm.form, false, Some(request.identity), activeOrgOpt))
    )
  }  

  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.edit")

    val futureResponses = for {
      //Get the uuid from the form data and see if it exists in the database
      futureOpOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      futureActiveOrg <- request.identity.activeOrganisation.isDefined match {
        case true  => organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
        case false => Future.successful(None)
      }
    } yield (futureOpOrg, futureActiveOrg)

    futureResponses.map(responses => responses._1 match {
      case Some(organisation) => {
        Ok(views.html.organisations.edit(OrganisationForm.form.fill(organisation), true, Some(request.identity), responses._2))
      }
      case None =>
        val orgForm = OrganisationForm.form
        Ok(views.html.organisations.edit(orgForm, false, Some(request.identity), responses._2))
    })
  }

  def editActiveOrganisation(organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editActivatedOrganisation")
    
    val selector = Organisation(allowedUsers = request.identity.uuid.toSet)
    
    val futureResponses = for {
      futureCount <- organisationService.count(selector)
      futureOrgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield(futureOrgList, futureCount, activeOrg)

    futureResponses.map( responses =>
      Ok(views.html.organisations.editActivateOrganisation(responses._1, responses._2, organisationsPage, 
          utils.DefaultValues.DefaultPageLength, Some(request.identity), responses._3)))
  }

  def setActiveOrganisation(uuid: String, organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    
    val selector = Organisation(allowedUsers = request.identity.uuid.toSet)
    
    val futureResponses = for {
      //Get the uuid from the form data and see if it exists in the database
      futureOpOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      futureActiveOrg <- request.identity.activeOrganisation.isDefined match {
        case true  => organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
        case false => Future.successful(None)
      }
      updatedUser <- {
        val newActiveOrg = if (request.identity.activeOrganisation.isDefined &&
          request.identity.activeOrganisation.get == uuid) {
          None
        } else {
          Some(uuid)
        } 
        
        val newUser = request.identity.copy(activeOrganisation = newActiveOrg) 
        
        userService.save(newUser)
      }
      futureCount <- organisationService.count(selector)
      futureOrgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
    } yield (futureOpOrg, futureActiveOrg, updatedUser, futureOrgList, futureCount)
    
    futureResponses.map( responses =>
    
    responses._3 match {
      case Some(updateUser) =>

        Ok(views.html.organisations.editActivateOrganisation(responses._4, responses._5,
          organisationsPage, utils.DefaultValues.DefaultPageLength, Some(updateUser), responses._2))
      case None =>
        Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
    }
    )
  }

  def editAllowedUsers(uuid: String, usersPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editAllowedUsers")
    
    val futureResponses = for {
      futureOpOrg <- organisationService.find(Organisation(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      userCount <- userService.count(User())
      futureUserList <- userService.find(User(), usersPage, utils.DefaultValues.DefaultPageLength)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield(futureOpOrg, futureUserList, userCount, activeOrg)

    futureResponses.map( responses =>
    
    responses._1 match {
      case Some(organisation) => {
        Ok(views.html.organisations.editAllowedUsers(organisation, responses._2, responses._3, usersPage, 
            utils.DefaultValues.DefaultPageLength, Some(request.identity), responses._4))
      }
      case None =>
        Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("db.error.read"))
    }
    )
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
    } yield(opOrg, orgUpdate)
    
    futureResponses.map( responses =>
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
      }
      )
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
    } yield(opOrg, deletedOrg)

    futureResponses.map( responses =>
      Redirect(routes.OrganisationController.list(1)).flashing(responses._2._2))
  }

  /*
  def image(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationDAO.findOneById(uuid) match {
      case Some(organisation) => {
        Organisation.getImageFile(organisation) match {
          case Some(imageFile) => {
            Future.successful(Ok.sendFile(content = imageFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.read.file", uuid)))
        }
      }
      case None => Future.successful(Redirect(routes.OrganisationController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def thumbnail(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationDAO.findOneById(uuid) match {
      case Some(organisation) => {
        Organisation.getThumbnailFile(organisation) match {
          case Some(thumbnailFile) => {
            Future.successful(Ok.sendFile(content = thumbnailFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.read.file", uuid)))
        }
      }
      case None => Future.successful(Redirect(routes.OrganisationController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }
  * 
  */
}
