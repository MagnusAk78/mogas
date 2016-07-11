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
import forms.UserForm

@Singleton
class UserController @Inject() (
    val messagesApi: MessagesApi, 
    val silhouette: Silhouette[DefaultEnv],
    val organisationService: OrganisationService, 
    val userService: UserService,
    implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext) 
      extends Controller with I18nSupport {
  
  

  def list(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("UserController.list")
    
    val futureResponses = for {
      activeOrgOpt <- {
        if(request.identity.activeOrganisation.isDefined) {
          organisationService.find(Organisation(uuid = request.identity.activeOrganisation), page, utils.DefaultValues.DefaultPageLength).map(_.headOption)
        } else {
          Future.successful(None)
        }
      }
      userList <- {
        //Todo: add get uuid from string
        userService.find(User(activeOrgOpt.get.allowedUsers.headOption))
      }
      //Todo: add count get uuid from string
      userCount <- userService.count(User())
    } yield(activeOrgOpt, userList, userCount)

    futureResponses map( responses =>
      
    if(responses._1.isDefined) {
      Ok(views.html.users.list(responses._2, responses._3, page, utils.DefaultValues.DefaultPageLength,
        Some(request.identity), responses._1))
    } else {
      Ok(views.html.users.list(List(), 0, page, utils.DefaultValues.DefaultPageLength,
        Some(request.identity), responses._1))
    })
  }

  def show(uuid: String, organisationPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async {
    implicit request =>
      Logger.info("UserController.show userObjectIdString: " + uuid)
      
    val futureResponses = for {
      userOpt <- userService.find(User(uuid = Some(uuid))).map(_.head)
      orgList <- {
        //Todo: add get uuid from string
        organisationService.find(Organisation(allowedUsers = Set(uuid)))
      }
      //Todo: add count get uuid from string
      orgCount <- organisationService.count(Organisation(allowedUsers = Set(uuid)))
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield(userOpt, orgList, orgCount, activeOrg)
    
    futureResponses map( responses =>

      Ok(views.html.users.details(responses._1, responses._2, responses._3, organisationPage, 
        utils.DefaultValues.DefaultPageLength, Some(responses._1), responses._4))
      )
  }
  
  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("UserController.edit")

    val futureResponses = for {
      //Get the uuid from the form data and see if it exists in the database
      userOpt <- userService.find(User(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
      activeOrg <- organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
    } yield (userOpt, activeOrg)

    futureResponses.map(responses => responses._1 match {
      case Some(user) => 
        Ok(views.html.users.editUser(UserForm.form.fill(user), Some(request.identity), responses._2))
      case None =>
        Redirect(routes.UserController.list(1))
    })
  }
  
  def submit = silhouette.SecuredAction(AlwaysAuthorized()).async(parse.multipartFormData) { implicit request =>
    Logger.info("UserController.save")

    val responses = for {
      //Get the uuid from the form data and see if it exists in the database
      opUser <- request.body.asFormUrlEncoded.get("uuid") match {
        case Some(uuid :: ignoringTheTail) => userService.find(User(uuid = Some(uuid)), maxDocs = 1).map(_.headOption)
        case _                                 => Future.successful(None)
      }
      activeOrg <- request.identity.activeOrganisation.isDefined match {
        case true  => organisationService.find(Organisation(uuid = request.identity.activeOrganisation)).map(_.headOption)
        case false => Future.successful(None)
      }
      
    } yield (opUser, activeOrg)

    responses.flatMap(responseTuple =>

      UserForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.users.editUser(formWithErrors, Some(request.identity),
            responseTuple._2)))
        },
        formData => {
          
          //val imageFileReps = Organisation.saveImageFile(request.body.file(models.imageFileKeyString))

          val newUser = responseTuple._1.get.copy(firstName = formData.firstName, lastName = formData.lastName, 
              fullName = formData.getFullName, email = formData.email)
          
          /*  
          uuid = formData.uuid,
            name = formData.name,
            allowedUsers = responseTuple._1 map (_.allowedUsers) getOrElse (Set(request.identity.uuid.get)) /*,
          imageFileRep = imageFileReps._1.orElse(organisationOpt.flatMap(_.imageFileRep)),
          thumbnailFileRep = imageFileReps._2.orElse(organisationOpt.flatMap(_.thumbnailFileRep)) */ )

          val futureSaveResult = userService.save(newUser)

          futureSaveResult map {
            optNewOrg =>
              optNewOrg.isDefined match {
                case true => Redirect(routes.OrganisationController.edit(optNewOrg.map(_.uuid.get).get)).
                  flashing("success" -> Messages("db.success.update", newUser.fullName))
                case false => Redirect(routes.OrganisationController.edit(formData.uuid.get)).
                  flashing("failure" -> Messages("db.success.update", newUser.fullName))
              }

          }

        }))*/
       Future.successful(Redirect(routes.UserController.list(1)))
      }
    )
  )}  
  
/*
  def image(uuid: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    UserDAO.findOneById(uuid) match {
      case Some(user) => {
        User.getImageFile(user) match {
          case Some(imageFile) => {
            Future.successful(Ok.sendFile(content = imageFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.UserController.list(1)).
              flashing("error" -> Messages("db.error.read.file", uuid)))
        }
      }
      case None => Future.successful(Redirect(routes.UserController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def thumbnail(uuid: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    UserDAO.findOneById(uuid) match {
      case Some(user) => {
        User.getThumbnailFile(user) match {
          case Some(thumbnailFile) => {
            Future.successful(Ok.sendFile(content = thumbnailFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.UserController.list(1)).
              flashing("error" -> Messages("db.error.read.file", uuid)))
        }
      }
      case None => Future.successful(Redirect(routes.UserController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }*/
}
