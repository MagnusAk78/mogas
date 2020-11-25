package controllers

import akka.stream.Materializer
import controllers.actions._
import javax.inject.{Inject, Singleton}
import models.formdata.DomainForm
import models.formdata.DomainForm.fromDomainToData
import models.services._
import models.{DbModel, Domain, HasParent}
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.DefaultValues
import viewdata.{HierarchyData, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomainController @Inject() (
  generalActions: GeneralActions,
  userService: UserService,
  domainService: DomainService,
  fileService: FileService,
  amlObjectService: AmlObjectService,
  instructionService: InstructionService,
  issueService: IssueService,
  components: ControllerComponents)(implicit exec: ExecutionContext, materialize: Materializer)
    extends AbstractController(components) with I18nSupport {
  implicit val lang: Lang = components.langs.availables.head

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
          imageExists <- fileService.imageExists(uuid)
          instructionListData <- instructionService.findManyInstructions(HasParent.queryByParent(elementRequest.elementChain.last.uuid))
        } yield Ok(views.html.browse.element(ElementData(elementRequest.myDomain, elementRequest.hierarchy,
          elementRequest.elementChain), imageExists, elementListData, interfaceListData, instructionListData, 
          UserStatus(Some(elementRequest.identity), elementRequest.activeDomain)))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def interface(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveDomain andThen
      generalActions.InterfaceAction(uuid)).async { implicit interfaceRequest =>
        
        val responses = for {
          imageExists <- fileService.imageExists(uuid)
          instructionListData <- instructionService.findManyInstructions(HasParent.queryByParent(interfaceRequest.interface.uuid))
        } yield Ok(views.html.browse.interface(interfaceRequest.interface, imageExists, 
            ElementData(interfaceRequest.myDomain, interfaceRequest.hierarchy, interfaceRequest.elementChain), 
            instructionListData, UserStatus(Some(interfaceRequest.identity), interfaceRequest.activeDomain)))
        
        responses recover {
          case e => InternalServerError(e.getMessage())
        }        
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
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).async { implicit domainRequest =>
      
      for {
        imageExists <- fileService.imageExists(uuid)
        amlFiles <- fileService.amlFiles(uuid)
      } yield Ok(views.html.domains.edit(DomainData(domainRequest.myDomain, imageExists, amlFiles), 
          DomainForm.form.fill(domainRequest.myDomain), 
          UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))
    }

  def show(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).async { implicit domainRequest =>
      
      for {
        imageExists <- fileService.imageExists(uuid)
      } yield Ok(views.html.domains.show(domainRequest.myDomain, imageExists, 
          UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))
    }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen generalActions.DomainAction(uuid)).
    async { implicit domainRequest =>
      DomainForm.form.bindFromRequest().fold(
        formWithErrors => {
          for {
            imageExists <- fileService.imageExists(uuid)
            amlFiles <- fileService.amlFiles(uuid)
          } yield BadRequest(views.html.domains.edit(DomainData(domainRequest.myDomain, imageExists, amlFiles), formWithErrors,
            UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))
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
            case true => domainService.findOneDomain(DbModel.queryByUuid(updateUser.activeDomain))
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
        userListData <- userService.findMany(DbModel.queryAll, page, DefaultValues.DefaultPageLength)
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
      } else if (!newDomain.allowedUsers.contains(domainRequest.identity.uuid)) {
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

