package controllers

import javax.inject.Inject
import com.google.inject.Singleton
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mongodb.casbah.Imports._

import models._
import play.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Controller


import scala.concurrent.Future
import models.OrganisationConversions._

@Singleton
class UserController @Inject() (val messagesApi: MessagesApi, val env: Environment[User, CookieAuthenticator])
  extends Controller with I18nSupport with Silhouette[User, CookieAuthenticator] with UserAuthorization {

  def list(page: Int) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("UserController.list")

    val orgOpt: Option[Organisation] = if(request.identity.activeOrganisation.isDefined) {
      Logger.info("request.identity.activeOrganisation.get: " + request.identity.activeOrganisation.get.toString)
      OrganisationDAO.findOneById(request.identity.activeOrganisation.get)
    } else {
      Option.empty[Organisation]
    }

    if(orgOpt.isDefined) {
      val organisation = orgOpt.get
      val usersCursor = UserDAO.find(DbHelper.queryIdFromIdList(organisation.allowedUsers))
      val count = usersCursor.count
      val users = DbHelper.paginate(usersCursor, page, models.defaultPageLength).toList
      Future.successful(Ok(views.html.users.list(users, count, page, models.defaultPageLength,
        Some(request.identity))))
    } else {
      Future.successful(Ok(views.html.users.list(List(), 0, page, models.defaultPageLength,
        Some(request.identity))))
    }
  }

  def show(userObjectIdString: String, organisationPage: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>
      Logger.info("UserController.show userObjectIdString: " + userObjectIdString)

      UserDAO.findOneById(userObjectIdString) match {
        case Some(user) => {
          Logger.info("UserController.show user: " + user.toString)

          val orgCursor = OrganisationDAO.find(OrganisationParams(allowedUsers = Some(user._id)))
          val count = orgCursor.count
          val organisationList = DbHelper.paginate(orgCursor, organisationPage, models.defaultPageLength).toList

          Future.successful(Ok(views.html.users.details(user, organisationList, count,
            organisationPage, models.defaultPageLength, Some(user))))
        }
        case None => Future.successful(Redirect(routes.UserController.list(1)).
          flashing("error" -> Messages("db.read.error")))
      }
  }

  def image(idString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    UserDAO.findOneById(idString) match {
      case Some(user) => {
        User.getImageFile(user) match {
          case Some(imageFile) => {
            Future.successful(Ok.sendFile(content = imageFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.UserController.list(1)).
              flashing("error" -> Messages("db.error.read.file", idString)))
        }
      }
      case None => Future.successful(Redirect(routes.UserController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def thumbnail(idString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    UserDAO.findOneById(idString) match {
      case Some(user) => {
        User.getThumbnailFile(user) match {
          case Some(thumbnailFile) => {
            Future.successful(Ok.sendFile(content = thumbnailFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.UserController.list(1)).
              flashing("error" -> Messages("db.error.read.file", idString)))
        }
      }
      case None => Future.successful(Redirect(routes.UserController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }
}
