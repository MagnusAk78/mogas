package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import utils.auth.DefaultEnv

import scala.concurrent.Future
import reactivemongo.core.nodeset.Authentication
import com.mohiva.play.silhouette.api.actions.SecuredAction
import com.mohiva.play.silhouette.api.actions.UserAwareAction
import models.User
import play.api.i18n.Lang
import com.mohiva.play.silhouette.api.actions.SecuredAction
import com.mohiva.play.silhouette.api.actions.UserAwareAction
import reactivemongo.core.nodeset.Authentication
import scala.concurrent.ExecutionContext
import models.services.UserService
import play.api.Logger
import controllers.actions.GeneralActions
import models.Domain
import models.services.DomainService

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param silhouette The Silhouette stack.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets The webjar assets implementation.
 */
class ApplicationController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv],
  val generalActions: GeneralActions,
  val socialProviderRegistry: SocialProviderRegistry,
  val userService: UserService,
  val domainService: DomainService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext)
    extends Controller with I18nSupport {

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = generalActions.MyUserAwareAction.async { implicit request =>

    val responses = for {
      userOpt <- request.identity match {
        case Some(user) => Future.successful(Some(user))
        case None => Future.successful(None)
      }
      activeDomain <- userOpt match {
        case Some(user) => domainService.findOneDomain(Domain.queryByUuid(user.activeDomain))
        case None => Future.successful(None)
      }
    } yield userOpt match {
      case Some(user) => Ok(views.html.welcome(userOpt, activeDomain))
      case None => Redirect(routes.SignInController.view())
    }

    responses recover {
      case e => InternalServerError(e.getMessage())
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = generalActions.MySecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  def changeLanguage(languageString: String) = generalActions.MyUserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index).withLang(Lang(languageString)))
      case None => Future.successful(Redirect(routes.SignInController.view).withLang(Lang(languageString)))
    }
  }
}