package controllers

import akka.stream.Materializer
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import controllers.actions.GeneralActions
import javax.inject.Inject
import models.formdata.SignUpForm
import models.services.{DomainService, FileService, UserService}
import models.{DbModel, User}
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{AbstractController, ControllerComponents}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import utils.auth.DefaultEnv
import viewdata._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Sign Up` controller.
 *
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService The avatar service implementation.
 * @param passwordHasher The password hasher implementation.
 */
class SignUpController @Inject() (
  silhouette: Silhouette[DefaultEnv],
  generalActions: GeneralActions,
  userService: UserService,
  domainService: DomainService,
  authInfoRepository: AuthInfoRepository,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  val reactiveMongoApi: ReactiveMongoApi,
  fileService: FileService,
  components: ControllerComponents)(implicit exec: ExecutionContext, materialize: Materializer)
    extends AbstractController(components) with MongoController with ReactiveMongoComponents with I18nSupport {

  implicit val lang: Lang = components.langs.availables.head

  val signUpControllerLogger: Logger = Logger("SignUpController")

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }

  def edit(uuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    signUpControllerLogger.info("SignUpController.edit")

    val responses = for {
      userOpt <- userService.findOne(DbModel.queryByUuid(uuid))
      activeDomainOpt <- domainService.findOneDomain(DbModel.queryByUuid(request.identity.activeDomain))
      imageExists <- fileService.imageExists(uuid)
    } yield userOpt match {
      case Some(user) => {
        if (user.uuid == request.identity.uuid) {
          //TODO: Use sign up form and edit
          Ok(views.html.users.edit(user, imageExists, SignUpForm.form.fill(user), UserStatus(Some(request.identity), 
              activeDomainOpt)))
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
      errorForm => Future.successful(BadRequest(views.html.signUp(errorForm))),
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

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.UserAction(uuid)).async { implicit userRequest =>
      SignUpForm.form.bindFromRequest.fold(
        errorForm => for {
            imageExists <- fileService.imageExists(uuid)
          } yield BadRequest(views.html.users.edit(userRequest.user, imageExists, errorForm,
          UserStatus(Some(userRequest.identity), None))),
        data => {

          val newLoginInfo = LoginInfo(CredentialsProvider.ID, data.email)
          val newUser = userRequest.user.copy(loginInfo = newLoginInfo, firstName = data.firstName,
            lastName = data.lastName, email = data.email)

          val newAuthInfo = passwordHasher.hash(data.password)

          for {
            avatar <- avatarService.retrieveURL(data.email)
            userUpdateResult <- userService.update(newUser.copy(avatarURL = avatar))
            authInfo <- authInfoRepository.add(newLoginInfo, newAuthInfo)
            authenticator <- silhouette.env.authenticatorService.create(newLoginInfo)
            value <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(value, Redirect(routes.ApplicationController.index))
          } yield {
            Redirect(routes.SignUpController.edit(uuid))
          }
        })
    }
}