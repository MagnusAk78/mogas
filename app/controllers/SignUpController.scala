package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import forms.SignUpForm
import models.User
import models.Organisation
import models.services.UserService
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.Future
import models.services.OrganisationService
import play.api.Logger
import models.Organisation

/**
 * The `Sign Up` controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 * @param webJarAssets The webjar assets implementation.
 */
class SignUpController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv],
  val userService: UserService,
  val organisationService: OrganisationService,
  val authInfoRepository: AuthInfoRepository,
  val avatarService: AvatarService,
  val passwordHasher: PasswordHasher,
  implicit val webJarAssets: WebJarAssets)
  extends Controller with I18nSupport {

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form, None, None)))
  }
  
  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("SignUpController.edit")

    val responses = for {
      userOpt <- userService.find(User.uuidQuery(uuid)).map(_.headOption)
      activeOrgOpt <- organisationService.find(Organisation.uuidQuery(request.identity.activeOrganisation)).map(_.headOption)
    } yield userOpt match {
      case Some(user) => {
        if(user.uuid == request.identity.uuid) {
          //TODO: Use sign up form and edit
          Ok(views.html.signUp(SignUpForm.form.fill(user), Some(request.identity), activeOrgOpt))
        } else {
        Redirect(routes.UserController.list(1))
        }
      }
      case None =>
        Redirect(routes.UserController.list(1))
    }

      responses recover {
        case e => InternalServerError(e.getMessage())
      }    
  }  

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      errorForm => Future.successful(BadRequest(views.html.signUp(errorForm, None, None))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            Future.successful(Redirect(routes.SignUpController.view()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val newUser = User.create(
              loginInfo = loginInfo,
              firstName = data.firstName,
              lastName = data.lastName,
              fullName = data.firstName + " " + data.lastName,
              email = data.email)
            for {
              avatar <- avatarService.retrieveURL(data.email)
              user <- userService.insert(newUser.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index))
            } yield {
              //ugly. no error handling for .get method
              silhouette.env.eventBus.publish(SignUpEvent(user.get, request))
              silhouette.env.eventBus.publish(LoginEvent(user.get, request))
              result
            }
        }
      }
    )
  }
  
    def editSubmit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      errorForm => Future.successful(BadRequest(views.html.signUp(errorForm, Some(request.identity), None))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            Future.successful(Redirect(routes.SignUpController.view()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val newUser = User.create(
              loginInfo = loginInfo,
              firstName = data.firstName,
              lastName = data.lastName,
              fullName = data.firstName + " " + data.lastName,
              email = data.email)
            for {
              avatar <- avatarService.retrieveURL(data.email)
              user <- userService.insert(newUser.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index))
            } yield {
              //ugly. no error handling for .get method
              silhouette.env.eventBus.publish(SignUpEvent(user.get, request))
              silhouette.env.eventBus.publish(LoginEvent(user.get, request))
              result
            }
        }
      }
    )
  }
}