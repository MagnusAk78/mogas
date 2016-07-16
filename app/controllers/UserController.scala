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
import utils.PaginateData
import models.services.FileService

@Singleton
class UserController @Inject() (
    val messagesApi: MessagesApi, 
    val silhouette: Silhouette[DefaultEnv],
    val organisationService: OrganisationService, 
    val userService: UserService,
    val fileService: FileService,
    implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext) 
      extends Controller with I18nSupport {
  
  def list(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("UserController.list")
    
    val responses = for {
      activeOrgOpt <- organisationService.findOneByUuid(request.identity.activeOrganisation)
      userList <- activeOrgOpt match {
        case Some(activeOrg) => userService.find(BaseModel.uuidInSetQuery(activeOrg.allowedUsers), page, utils.DefaultValues.DefaultPageLength)
        case None => Future.successful(List.empty[User])
      }
      userCount <- activeOrgOpt match {
        case Some(activeOrg) => userService.count(BaseModel.uuidInSetQuery(activeOrg.allowedUsers))
        case None => Future.successful(0)
      }
    } yield Ok(views.html.users.list(userList, PaginateData(page, userCount), Some(request.identity), activeOrgOpt))

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  def show(uuid: String, page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async {
    implicit request =>
      Logger.info("UserController.show userObjectIdString: " + uuid)
      
    val responses = for {
      activeOrgOpt <- organisationService.findOneByUuid(request.identity.activeOrganisation)
      userOpt <- userService.findOneByUuid(uuid)
      orgList <- userOpt match {
        case Some(user) => {
          Logger.info("UserController show user: " + user)
          organisationService.find(Organisation.allowedUserQuery(user.uuid), page, utils.DefaultValues.DefaultPageLength)
        }
        case None => Future.successful(List.empty[Organisation])
      }
      orgCount <- userOpt match {
        case Some(user) => organisationService.count(Organisation.allowedUserQuery(user.uuid))
        case None => Future.successful(0)
      }
    } yield Ok(views.html.users.details(userOpt.get, orgList, PaginateData(page, orgCount), Some(request.identity), activeOrgOpt))
    
      responses recover {
        case e => InternalServerError(e.getMessage())
      }    
  }
}
