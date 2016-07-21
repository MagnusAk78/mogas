package controllers

import scala.concurrent.Future

import org.joda.time.DateTime

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import com.sksamuel.scrimage.Image

import forms.SignUpForm
import javax.inject.Inject
import models.Organisation
import models.Images
import models.User
import models.daos.FileDAO
import models.services.FileService
import models.services.OrganisationService
import models.services.UserService
import play.api.Logger
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsObject
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import play.api.mvc.Request
import play.modules.reactivemongo.JSONFileToSave
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.ReactiveMongoComponents
import reactivemongo.play.json.JsFieldBSONElementProducer
import reactivemongo.api.gridfs.GridFS
import reactivemongo.api.gridfs.ReadFile
import utils.auth.DefaultEnv
import akka.stream.Materializer
import play.api.libs.iteratee.Iteratee
import controllers.actions.GeneralActions

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
  val generalActions: GeneralActions,
  val userService: UserService,
  val organisationService: OrganisationService,
  val authInfoRepository: AuthInfoRepository,
  val avatarService: AvatarService,
  val passwordHasher: PasswordHasher,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val webJarAssets: WebJarAssets)(implicit materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

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
      userOpt <- userService.findOneByUuid(uuid)
      activeOrgOpt <- organisationService.findOneByUuid(request.identity.activeOrganisation)
    } yield userOpt match {
      case Some(user) => {
        if (user.uuid == request.identity.uuid) {
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
  def submitSignUp = silhouette.UnsecuredAction.async { implicit request =>
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
              name = data.firstName + " " + data.lastName,
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
      })
  }

  def submitEdit(uuid: String) = generalActions.MySecuredAction.async { implicit request =>
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
              name = data.firstName + " " + data.lastName,
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
      })
  }
}