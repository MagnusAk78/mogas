package controllers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Silhouette

import javax.inject.Inject
import javax.inject.Singleton
import models.DbModel
import models.Organisation
import models.User
import models.services.OrganisationService
import models.services.UserService
import play.Logger
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import utils.PaginateData
import utils.auth.DefaultEnv
import controllers.actions.GeneralActions
import controllers.actions.MySecuredRequest

@Singleton
class UserController @Inject() (
  val messagesApi: MessagesApi,
  val organisationService: OrganisationService,
  val userService: UserService,
  val generalActions: GeneralActions,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext)
    extends Controller with I18nSupport {

  def list(page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>
      val responses = for {
        userListData <- userService.getUserList(page, mySecuredRequest.activeOrganisation.map(activeOrg =>
            activeOrg.allowedUsers).getOrElse(Set()))
      } yield Ok(views.html.users.list(userListData.list, userListData.paginateData,
        Some(mySecuredRequest.identity), mySecuredRequest.activeOrganisation))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def show(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.UserAction(uuid)).async { implicit userRequest =>
      val responses = for {
        organisationList <- organisationService.getOrganisationList(page, uuid)
      } yield Ok(views.html.users.details(userRequest.user, organisationList.list, organisationList.paginateData,
        Some(userRequest.identity), userRequest.activeOrganisation))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }
}
