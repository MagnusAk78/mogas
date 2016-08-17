package controllers

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import akka.stream.Materializer
import models.formdata.DomainForm
import models.formdata.DomainForm.fromDomainToData
import javax.inject.Inject
import javax.inject.Singleton
import models.services.DomainService
import models.services.UserService
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import utils.PaginateData
import utils.auth.DefaultEnv
import models.Images
import play.api.mvc.WrappedRequest
import play.api.mvc.Request
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import play.api.mvc.ActionRefiner
import play.api.mvc.ActionTransformer
import play.api.mvc.ActionBuilder
import play.api.mvc.Flash
import controllers.actions._
import utils.RemoveResult
import models.formdata.DomainForm
import models.Domain
import models.services.AmlObjectService
import play.api.Logger
import viewdata.HierarchyData
import viewdata._

@Singleton
class DomainController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val userService: UserService,
  val domainService: DomainService,
  val amlObjectService: AmlObjectService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(page: Int) =
    (generalActions.MySecuredAction).async { implicit mySecuredRequest =>
      val responses = for {
        domainListData <- domainService.getDomainList(page, mySecuredRequest.identity)
      } yield Ok(views.html.domains.list(domainListData, UserStatus(Some(mySecuredRequest.identity),
        mySecuredRequest.activeDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def domain(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).async { implicit domainRequest =>

      val responses = for {
        hierarchyListData <- domainService.getHierarchyList(page, domainRequest.myDomain)
      } yield Ok(views.html.browse.domain(domainRequest.myDomain, hierarchyListData,
        UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def hierarchy(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveDomain andThen
      generalActions.HierarchyAction(uuid)).async { implicit hierarchyRequest =>

        val responses = for {
          elementListData <- amlObjectService.getElementList(page, hierarchyRequest.hierarchy)
        } yield Ok(views.html.browse.hierarchy(HierarchyData(hierarchyRequest.myDomain, hierarchyRequest.hierarchy),
          elementListData, UserStatus(Some(hierarchyRequest.identity), hierarchyRequest.activeDomain)))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def element(uuid: String, elementPage: Int, interfacePage: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveDomain andThen
      generalActions.ElementAction(uuid)).async { implicit elementRequest =>

        val responses = for {
          elementListData <- amlObjectService.getElementList(elementPage, elementRequest.elementChain.last)
          interfaceListData <- amlObjectService.getInterfaceList(interfacePage, elementRequest.elementChain.last)
        } yield Ok(views.html.browse.element(elementRequest.myDomain, elementRequest.hierarchy,
          elementRequest.elementChain, elementListData, interfaceListData, UserStatus(Some(elementRequest.identity),
          elementRequest.activeDomain)))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def interface(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveDomain andThen
      generalActions.InterfaceAction(uuid)) { implicit interfaceRequest =>
        Ok(views.html.browse.interface(interfaceRequest.myDomain, interfaceRequest.hierarchy,
          interfaceRequest.elementChain, interfaceRequest.interface, UserStatus(Some(interfaceRequest.identity),
          interfaceRequest.activeDomain)))
      }

  def parseAmlFiles(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.DomainAction(uuid)).async { implicit domainRequest =>

      domainService.parseAmlFiles(domainRequest.myDomain).map { success =>
        success match {
          case true => Redirect(routes.DomainController.list(1)).flashing("success" -> Messages("domain.amlFilesParsed"))
          case false => Redirect(routes.DomainController.list(1)).flashing("error" -> Messages("domain.amlFilesParsed"))
        }
      }
    }

  def create = generalActions.MySecuredAction { implicit mySecuredRequest =>
    Ok(views.html.domains.create(DomainForm.form, UserStatus(Some(mySecuredRequest.identity), mySecuredRequest.activeDomain)))
  }

  def submitCreate = generalActions.MySecuredAction.async { implicit mySecuredRequest =>
    DomainForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.domains.create(formWithErrors, UserStatus(Some(mySecuredRequest.identity),
          mySecuredRequest.activeDomain))))
      },
      formData => {
        Logger.info("submitCreate: " + formData.toString)
        val responses = for {
          optSavedDomain <- domainService.insertDomain(Domain.create(name = formData.name,
            allowedUsers = Set(mySecuredRequest.identity.uuid)))
        } yield optSavedDomain match {
          case Some(newDomain) =>
            Redirect(routes.DomainController.edit(newDomain.uuid)).
              flashing("success" -> Messages("db.success.insert", newDomain.name))
          case None =>
            Redirect(routes.DomainController.create).
              flashing("failure" -> Messages("db.failure.insert", formData.name))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      })
  }

  def edit(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)) { implicit domainRequest =>

      Ok(views.html.domains.edit(domainRequest.myDomain, DomainForm.form.fill(domainRequest.myDomain),
        UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))
    }

  def show(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)) { implicit domainRequest =>

      Ok(views.html.domains.show(domainRequest.myDomain, UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))
    }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).
    async { implicit domainRequest =>
      DomainForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.domains.edit(domainRequest.myDomain, formWithErrors,
            UserStatus(Some(domainRequest.identity), domainRequest.activeDomain))))
        },
        formData => {
          val responses = for {
            updateResult <- {
              val updateDomain = domainRequest.myDomain.copy(name = formData.name)
              domainService.updateDomain(updateDomain)
            }
          } yield updateResult match {
            case true =>
              Redirect(routes.DomainController.list(1)).
                flashing("success" -> Messages("db.success.update", formData.name))
            case false =>
              Redirect(routes.DomainController.edit(uuid)).
                flashing("failure" -> Messages("db.failure.update", formData.name))
          }

          responses recover {
            case e => InternalServerError(e.getMessage())
          }
        })
    }

  def delete(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.DomainAction(uuid)).async { implicit domainRequest =>
      val responses = for {
        removeResult <- domainService.removeDomain(domainRequest.myDomain, domainRequest.identity.uuid)
      } yield if (removeResult.success) {
        Redirect(routes.DomainController.list(1)).flashing("success" -> Messages("db.success.remove", domainRequest.myDomain.name))
      } else {
        Redirect(routes.DomainController.list(1)).flashing("error" -> removeResult.getReason)
      }

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editActiveDomain(page: Int) =
    generalActions.MySecuredAction.async { implicit mySecuredRequest =>
      val responses = for {
        domainListData <- domainService.getDomainList(page, mySecuredRequest.identity)
      } yield Ok(views.html.domains.editActivateDomain(domainListData, UserStatus(Some(mySecuredRequest.identity),
        mySecuredRequest.activeDomain)))
      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def setActiveDomain(uuid: String, page: Int) =
    (generalActions.MySecuredAction).async { implicit mySecuredRequest =>

      val responses = for {
        newActiveDomain <- {
          val updateUser = if (mySecuredRequest.activeDomain.isDefined && mySecuredRequest.activeDomain.get.uuid == uuid) {
            mySecuredRequest.identity.copy(activeDomain = models.UuidNotSet)
          } else {
            mySecuredRequest.identity.copy(activeDomain = uuid)
          }
          userService.update(updateUser).flatMap(b => b match {
            case true => domainService.findOneDomain(Domain.queryByUuid(updateUser.activeDomain))
            case false => Future.successful(None)
          })
        }
        domainListData <- domainService.getDomainList(page, mySecuredRequest.identity)
      } yield Ok(views.html.domains.editActivateDomain(domainListData, UserStatus(Some(mySecuredRequest.identity),
        newActiveDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def editAllowedUsers(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).async { implicit domainRequest =>
      val responses = for {
        userListData <- userService.getUserList(page, domainRequest.myDomain.allowedUsers)
      } yield Ok(views.html.domains.editAllowedUsers(domainRequest.myDomain, userListData, UserStatus(Some(domainRequest.identity),
        domainRequest.activeDomain)))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def changeAllowedUser(uuid: String, userUuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).async { implicit domainRequest =>

      val newDomain = domainRequest.myDomain.copy(allowedUsers = domainRequest.myDomain.allowedUsers.contains(userUuid) match {
        case true => domainRequest.myDomain.allowedUsers - userUuid
        case false => domainRequest.myDomain.allowedUsers + userUuid
      })

      if (newDomain.allowedUsers.isEmpty) {
        //This is not ok, some user must be allowed to see/change it
        Future.successful(Redirect(routes.DomainController.editAllowedUsers(uuid, page)).
          flashing("error" -> Messages("thereMustBeAtLeastOneAllowedUser")))
      } else if (newDomain.allowedUsers.contains(domainRequest.identity.uuid) == false) {
        //This is not ok, the logged in user must be part of the domain.
        Future.successful(Redirect(routes.DomainController.editAllowedUsers(uuid, page)).
          flashing("error" -> Messages("accessDenied")))
      } else {
        val responses = for {
          //Update the domain
          updateSuccess <- domainService.updateDomain(newDomain)
        } yield if (updateSuccess) {
          Redirect(routes.DomainController.editAllowedUsers(newDomain.uuid, page)).
            flashing("success" -> Messages("db.success.update", domainRequest.myDomain.name))
        } else {
          Redirect(routes.DomainController.editAllowedUsers(newDomain.uuid, page)).
            flashing("error" -> Messages("db.failure.update"))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }
    }
}

