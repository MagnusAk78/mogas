package controllers

import controllers.actions.GeneralActions
import javax.inject.{Inject, Singleton}
import models.services.{DomainService, FileService, UserService}
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{AbstractController, ControllerComponents}
import viewdata._

import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject() (
  domainService: DomainService,
  userService: UserService,
  generalActions: GeneralActions,
  fileService: FileService,
  components: ControllerComponents)(implicit exec: ExecutionContext)
    extends AbstractController(components) with I18nSupport {
  implicit val lang: Lang = components.langs.availables.head

  def list(page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>
      val responses = for {
        userListData <- userService.getUserList(page, mySecuredRequest.activeDomain.map(activeDomain =>
          activeDomain.allowedUsers).getOrElse(Set()))
      } yield Ok(views.html.users.list(userListData, UserStatus(Some(mySecuredRequest.identity), mySecuredRequest.activeDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def show(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.UserAction(uuid)).async { implicit userRequest =>
      val responses = for {
        domainListData <- domainService.getDomainList(page, userRequest.user)
        imageExists <- fileService.imageExists(uuid)
      } yield Ok(views.html.users.show(userRequest.user, imageExists, domainListData, 
          UserStatus(Some(userRequest.identity), userRequest.activeDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }
}
