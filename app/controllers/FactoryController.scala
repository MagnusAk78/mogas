package controllers

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import akka.stream.Materializer
import forms.OrganisationForm
import forms.OrganisationForm.fromOrganisationToData
import javax.inject.Inject
import javax.inject.Singleton
import models.Organisation
import models.services.FactoryService
import models.services.OrganisationService
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
import models.services.RemoveResult
import forms.FactoryForm
import models.Factory
import models.services.HierarchyService
import models.services.InternalElementService
import models.services.ExternalInterfaceService

@Singleton
class FactoryController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val userService: UserService,
  val organisationService: OrganisationService,
  val hierarchyService: HierarchyService,
  val internalElementService: InternalElementService,
  val externalInterfaceService: ExternalInterfaceService,
  val factoryService: FactoryService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(page: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation).async { implicit mySecuredRequest =>
      val responses = for {
        factoryListData <- factoryService.getFactoryList(page, mySecuredRequest.activeOrganisation.get.uuid)
      } yield Ok(views.html.factories.list(factoryListData.list, factoryListData.paginateData,
        Some(mySecuredRequest.identity), mySecuredRequest.activeOrganisation))

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }

  def factory(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.FactoryAction(uuid)).async { implicit factoryRequest =>

        val responses = for {
          hierarchyListData <- hierarchyService.getHierarchyList(page, factoryRequest.factory)
        } yield Ok(views.html.factories.factory(factoryRequest.factory, hierarchyListData.list, hierarchyListData.paginateData,
          Some(factoryRequest.identity), factoryRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def hierarchy(uuid: String, page: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.HierarchyAction(uuid)).async { implicit hierarchyRequest =>

        val responses = for {
          elementListData <- internalElementService.getInternalElementList(page, hierarchyRequest.hierarchy)
        } yield Ok(views.html.factories.hierarchy(hierarchyRequest.factory, hierarchyRequest.hierarchy,
          elementListData.list, elementListData.paginateData, Some(hierarchyRequest.identity),
          hierarchyRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def element(uuid: String, elementPage: Int, interfacePage: Int) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.ElementAction(uuid)).async { implicit elementRequest =>

        val responses = for {
          elementListData <- internalElementService.getInternalElementList(elementPage, elementRequest.elementChain.last)
          interfaceListData <- externalInterfaceService.getExternalInterfaceList(interfacePage, elementRequest.elementChain.last)
        } yield Ok(views.html.factories.element(elementRequest.factory, elementRequest.hierarchy,
          elementRequest.elementChain, elementListData.list, elementListData.paginateData, interfaceListData.list,
          interfaceListData.paginateData, Some(elementRequest.identity), elementRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      }

  def interface(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.InterfaceAction(uuid)) { implicit interfaceRequest =>
        Ok(views.html.factories.interface(interfaceRequest.factory, interfaceRequest.hierarchy,
          interfaceRequest.elementChain, interfaceRequest.interface, Some(interfaceRequest.identity),
          interfaceRequest.activeOrganisation))
      }

  def parseAmlFiles(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.FactoryAction(uuid)).
    async { implicit factoryRequest =>

      factoryService.parseAmlFiles(factoryRequest.factory).map { success =>
        success match {
          case true => Redirect(routes.FactoryController.list(1)).flashing("success" -> Messages("factory.amlFilesParsed"))
          case false => Redirect(routes.FactoryController.list(1)).flashing("error" -> Messages("factory.amlFilesParsed"))
        }
      }
    }

  def create = (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation) { implicit mySecuredRequest =>
    Ok(views.html.factories.edit(FactoryForm.form, None, Some(mySecuredRequest.identity), mySecuredRequest.activeOrganisation))
  }

  def submitCreate = (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation).async { implicit mySecuredRequest =>
    FactoryForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.factories.edit(formWithErrors, None, Some(mySecuredRequest.identity),
          mySecuredRequest.activeOrganisation)))
      },
      formData => {
        val responses = for {
          optSavedFactory <- factoryService.insert(Factory.create(name = formData.name, organisation = mySecuredRequest.activeOrganisation.get.uuid))
        } yield optSavedFactory match {
          case Some(newFactory) =>
            Redirect(routes.FactoryController.edit(newFactory.uuid)).
              flashing("success" -> Messages("db.success.insert", newFactory.name))
          case None =>
            Redirect(routes.FactoryController.create).
              flashing("failure" -> Messages("db.failure.insert", formData.name))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
      })
  }

  def edit(uuid: String) =
    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.FactoryAction(uuid)) { implicit factoryRequest =>

        Ok(views.html.factories.edit(FactoryForm.form.fill(factoryRequest.factory), Some(uuid), Some(factoryRequest.identity),
          factoryRequest.activeOrganisation))
      }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
    generalActions.FactoryAction(uuid)).async { implicit factoryRequest =>
      FactoryForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.factories.edit(formWithErrors, None, Some(factoryRequest.identity),
            factoryRequest.activeOrganisation)))
        },
        formData => {
          val responses = for {
            updateResult <- {
              val updateFactory = factoryRequest.factory.copy(name = formData.name)
              factoryService.update(updateFactory)
            }
          } yield updateResult match {
            case true =>
              Redirect(routes.FactoryController.list(1)).
                flashing("success" -> Messages("db.success.update", formData.name))
            case false =>
              Redirect(routes.FactoryController.edit(uuid)).
                flashing("failure" -> Messages("db.failure.update", formData.name))
          }

          responses recover {
            case e => InternalServerError(e.getMessage())
          }
        })
    }

  def delete(uuid: String) = (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
    generalActions.FactoryAction(uuid)).async { implicit factoryRequest =>
      val responses = for {
        removeResult <- factoryService.remove(factoryRequest.factory, factoryRequest.identity.uuid)
      } yield if (removeResult.success) {
        Redirect(routes.FactoryController.list(1)).flashing("success" -> Messages("db.success.remove", factoryRequest.factory.name))
      } else {
        Redirect(routes.FactoryController.list(1)).flashing("error" -> removeResult.getReason)
      }

      responses recover {
        case e => InternalServerError(e.getMessage())
      }
    }
}

/*
  def list(page: Int) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    request.identity.activeOrganisation match {
      case Some(objectId) =>

        val factoryCursor = FactoryDAO.find(FactoryParams(organisation = Some(objectId)))
        val count = factoryCursor.count
        val factories = DbHelper.paginate(factoryCursor, page, models.defaultPageLength).toList

        Future.successful(Ok(views.html.factories.list(factories, count, page, models.defaultPageLength,
          Some(request.identity))))


      case None => Future.successful(Ok(views.html.factories.list(List(), 0, page, models.defaultPageLength,
        Some(request.identity))))
    }
  }

  def create = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    request.identity.activeOrganisation match {
      case Some(org) => {
        val preFillFactory = Factory(
          _id = new ObjectId(),
          name = "",
          organisation = org,
          factoryHierachies = Set(),
          amlFileReps = List()
        )
        Future.successful(Ok(views.html.factories.edit(Factory.factoryForm.fill(preFillFactory), false,
          Some(request.identity))))
      }
      case None => {
        Future.successful(Redirect(routes.OrganisationController.list(1)).
          flashing("error" -> Messages("select.active.organisation")))
      }
    }
  }

  def edit(factoryIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    FactoryDAO.findOneById(factoryIdString) match {
      case Some(factory) => {
        Logger.info("About to edit factory:" + factory)
        //Make sure this user may edit
        OrganisationDAO.findOneById(factory.organisation).get.allowedUsers.contains(request.identity._id) match {
          case true =>
            Future.successful(Ok(views.html.factories.edit(Factory.factoryForm.fill(factory),
              true, Some(request.identity))))
          case false =>
            Future.successful(Redirect(routes.FactoryController.list(1)).
              flashing("error" -> Messages("access.denied")))
        }
      }
      case None => Future.successful(Redirect(routes.FactoryController.list(1)).
        flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
    }
  }

  def save = SecuredAction(AlwaysAuthorized()).async(parse.multipartFormData) { implicit request =>
    Logger.info("FactoryController.save")

    //Get the id from the form data and see if int exists in the database
    val storedFactoryOpt: Option[Factory] = request.body.asFormUrlEncoded.get("id") match {
      case Some(idString :: ignoringTheTail) => FactoryDAO.findOneById(idString)
      case _ => None
    }

    Factory.factoryForm.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.factories.edit(formWithErrors,
          storedFactoryOpt.isDefined, Some(request.identity))))
      },
      factoryForm => {
        val factoryToSave = factoryForm.copy(
          factoryHierachies = storedFactoryOpt.map(_.factoryHierachies).getOrElse(Set()),
          amlFileReps = Factory.saveAmlFile(request.body.file(models.amlFileKeyString)) match {
            case Some(newAmlRep) => List(newAmlRep)
            case None => storedFactoryOpt.map(_.amlFileReps).getOrElse(List())
          })

        if (OrganisationDAO.findOneById(factoryToSave.organisation).map(_.allowedUsers.toList).
          getOrElse(List.empty[ObjectId]).contains(request.identity._id) == false) {
          Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("access.denied")))
        } else {
          storedFactoryOpt match {
            case Some(storedFactory) => {
              FactoryDAO.update(FactoryParams(_id = Some(factoryToSave._id)), factoryToSave)
            }
            case None =>
              FactoryDAO.insert(factoryToSave)
          }

          FactoryAmlHelper.updateFactoryHierarchies(factoryToSave)

          Future.successful(Redirect(routes.FactoryController.edit(factoryToSave._id.toString)).
            flashing("success" -> Messages("db.success.save", factoryToSave.name)))
        }
      }
    )
  }

  def delete(factoryIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    FactoryDAO.findOneById(factoryIdString) match {
      case Some(factory) => {
        //Make sure this user may edit
        OrganisationDAO.findOneById(factory.organisation).get.allowedUsers.contains(request.identity._id) match {
          case true =>
            FactoryDAO.remove(factory)
            Future.successful(Redirect(routes.FactoryController.list(1)).
              flashing("success" -> Messages("db.success.remove", factory.name)))
          case false => Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("access.denied")))
        }
      }
      case None => {
        Future.successful(Redirect(routes.FactoryController.list(1)).
          flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
      }
    }
  }

  def factory(factoryIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    FactoryDAO.findOneById(factoryIdString) match {
      case Some(factory) => {
        HierarchyDbHelper.getFactoryHierarchies(factoryIdString) match {
          case Some(hierarchiesCursor) => {
            val count = hierarchiesCursor.count
            val hierarchies = DbHelper.paginate(hierarchiesCursor, page, models.defaultPageLength).toList
            Future.successful(Ok(views.html.factories.factory(factory, hierarchies, count, page, models.defaultPageLength,
              Some(request.identity))))
          }
          case None => Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
        }
      }
      case None => {
        Future.successful(Redirect(routes.FactoryController.list(1)).
          flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
      }
    }
  }

  def hierarchy(factoryIdString: String, hierarchyIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).
    async { implicit request =>
    FactoryDAO.findOneById(factoryIdString) match {
      case Some(factory) => {
        HierarchyDAO.findOneById(hierarchyIdString) match {
          case Some(hierarchy) => {
            InternalElementDbHelper.getInternalElements(Left(hierarchy)) match {
              case Some(elementsCursor) => {
                val count = elementsCursor.count
                val elements = DbHelper.paginate(elementsCursor, page, models.defaultPageLength).toList
                Future.successful(Ok(views.html.factories.hierarchy(factory, hierarchy, elements, count, page,
                  models.defaultPageLength, Some(request.identity))))
              }
              case None => Future.successful(Redirect(routes.FactoryController.list(1)).
                flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
            }
          }
          case None =>
            Future.successful(Redirect(routes.FactoryController.list(1)).
              flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
        }
      }
      case None => {
        Future.successful(Redirect(routes.FactoryController.list(1)).
          flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
      }
    }
  }

  def element(factoryIdString: String, hierarchyIdString: String, elementIdString: String, page: Int) =
    SecuredAction(AlwaysAuthorized()).async { implicit request =>
      FactoryDAO.findOneById(factoryIdString) match {
        case Some(factory) => {
          HierarchyDAO.findOneById(hierarchyIdString) match {
            case Some(hierarchy) => {
              InternalElementDAO.findOneById(elementIdString) match {
                case Some(internalElement) => {

                  InternalElementDbHelper.getInternalElements(Right(internalElement)) match {
                    case Some(elementsCursor) => {
                      val elementsCount = elementsCursor.count

                      val interfacesCursor = ExternalInterfaceDAO.find(DbHelper.queryIdFromIdList(internalElement.
                        externalInterfaces))
                      val interfacesCount = interfacesCursor.count

                      val count = elementsCount + interfacesCount

                      val elements = if (elementsCount > ((page - 1) * models.defaultPageLength)) {
                        DbHelper.paginate(elementsCursor, page, models.defaultPageLength).toList
                      } else {
                        List.empty[InternalElement]
                      }

                      val eiPageLength = models.defaultPageLength - elements.size
                      val interfaces = if (eiPageLength > 0) {
                        val lastToShow = models.defaultPageLength * page
                        var lastInterfaceToShow = lastToShow - elementsCount
                        var eiPage = 1
                        while(lastInterfaceToShow > models.defaultPageLength) {
                          lastInterfaceToShow = lastInterfaceToShow - models.defaultPageLength
                          eiPage = eiPage + 1
                        }
                        DbHelper.paginate(interfacesCursor, eiPage, eiPageLength).toList
                      } else {
                        List.empty[ExternalInterface]
                      }

                      Future.successful(Ok(views.html.factories.element(factory, hierarchy, internalElement,
                        elements, interfaces, count, page, models.defaultPageLength, Some(request.identity))))
                    }
                    case None => Future.successful(Redirect(routes.FactoryController.list(1)).
                      flashing("error" -> Messages("db.error.find", "Factory", elementIdString)))
                  }
                }
                case None => {
                  Future.successful(Redirect(routes.FactoryController.list(1)).
                    flashing("error" -> Messages("db.error.find", "Factory", elementIdString)))
                }
              }
            }
            case None =>
              Future.successful(Redirect(routes.FactoryController.list(1)).
                flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
          }
        }
        case None => {
          Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
        }
      }
    }

  def saveElementImage(factoryIdString: String, hierarchyIdString: String, elementIdString: String, page: Int) =
    SecuredAction(AlwaysAuthorized()).async(parse.multipartFormData) { implicit request =>
      Logger.info("FactoryController.saveElementImage")

      val ieOpt = InternalElementDAO.findOneById(elementIdString)

      if (ieOpt.isEmpty) {
        Future.successful(Redirect(routes.FactoryController.list(1)).
          flashing("error" -> Messages("db.error.find", "InternalElement", elementIdString)))
      } else {
        val imageFileReps = InternalElement.saveImageFile(request.body.file(models.imageFileKeyString))
        InternalElementDAO.update(InternalElementParams(_id = Some(ieOpt.get._id)),
          ieOpt.get.copy(imageFileRep = imageFileReps._1, thumbnailFileRep = imageFileReps._2))

        Future.successful(Redirect(routes.FactoryController.element(factoryIdString, hierarchyIdString, elementIdString,
          page)).flashing("success" -> Messages("db.success.save", elementIdString)))
      }
    }

  def elementImage(factoryIdString: String, hierarchyIdString: String, elementIdString: String) =
    SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("FactoryController.elementImage")

      FactoryDAO.findOneById(factoryIdString) match {
        case Some(factory) => {
          HierarchyDAO.findOneById(hierarchyIdString) match {
            case Some(hierarchy) => {
              InternalElementDAO.findOneById(elementIdString) match {
                case Some(internalElement) => {
                  InternalElement.getImageStream(internalElement) match {
                    case Some(imageStream) => {
                      Future.successful(Ok.stream(imageStream))
                    }
                    case None =>
                      Future.successful(Redirect(routes.OrganisationController.list(1)).
                        flashing("error" -> Messages("db.error.read.file", elementIdString)))
                  }
                }
                case None => {
                  Future.successful(Redirect(routes.FactoryController.list(1)).
                    flashing("error" -> Messages("db.error.find", "InternalElement", elementIdString)))
                }
              }
            }
            case None =>
              Future.successful(Redirect(routes.FactoryController.list(1)).
                flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
          }
        }
        case None => {
          Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
        }
      }
    }

  def elementThumbnail(factoryIdString: String, hierarchyIdString: String, elementIdString: String) =
    SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("FactoryController.elementImage")

      FactoryDAO.findOneById(factoryIdString) match {
        case Some(factory) => {
          HierarchyDAO.findOneById(hierarchyIdString) match {
            case Some(hierarchy) => {
              InternalElementDAO.findOneById(elementIdString) match {
                case Some(internalElement) => {
                  InternalElement.getThumbnailFile(internalElement) match {
                    case Some(thumbnailFile) => {
                      Future.successful(Ok.sendFile(content = thumbnailFile, inline = true))
                    }
                    case None =>
                      Future.successful(Redirect(routes.OrganisationController.list(1)).
                        flashing("error" -> Messages("db.error.read.file", elementIdString)))
                  }
                }
                case None => {
                  Future.successful(Redirect(routes.FactoryController.list(1)).
                    flashing("error" -> Messages("db.error.find", "InternalElement", elementIdString)))
                }
              }
            }
            case None =>
              Future.successful(Redirect(routes.FactoryController.list(1)).
                flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
          }
        }
        case None => {
          Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
        }
      }
    }

  def interface(factoryIdString: String, hierarchyIdString: String, elementIdString: String,
                interfaceIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    FactoryDAO.findOneById(factoryIdString) match {
      case Some(factory) => {
        HierarchyDAO.findOneById(hierarchyIdString) match {
          case Some(hierarchy) => {
            InternalElementDAO.findOneById(elementIdString) match {
              case Some(internalElement) => {
                ExternalInterfaceDAO.findOneById(interfaceIdString) match {
                  case Some(externalInterface) => {
                    Future.successful(Ok(views.html.factories.interface(factory, hierarchy, internalElement,
                      externalInterface, Some(request.identity))))
                  }
                  case None => {
                    Future.successful(Redirect(routes.FactoryController.list(1)).
                      flashing("error" -> Messages("db.error.find", "ExternalInterface", interfaceIdString)))
                  }
                }
              }
              case None =>
                Future.successful(Redirect(routes.FactoryController.list(1)).
                  flashing("error" -> Messages("db.error.find", "InternalElement", elementIdString)))
            }
          }
          case None =>
            Future.successful(Redirect(routes.FactoryController.list(1)).
              flashing("error" -> Messages("db.error.find", "Hierarchy", hierarchyIdString)))
        }
      }
      case None => {
        Future.successful(Redirect(routes.FactoryController.list(1)).
          flashing("error" -> Messages("db.error.find", "Factory", factoryIdString)))
      }
    }
  }

  def amlFile(factoryObjectId: String, fileObjectId: String) = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) =>
        FactoryDAO.findOneById(factoryObjectId) match {
          case Some(factory) => {
            Factory.getAmlFile(factory, fileObjectId) match {
              case Some(amlFile) => {
                Future.successful(Ok.sendFile(amlFile))
              }
              case None => Future.successful(Redirect(routes.FactoryController.list(1)).
                flashing("error" -> Messages("db.error.read.file", fileObjectId)))
            }
          }
          case None => Future.successful(Redirect(routes.FactoryController.list(1)).
            flashing("error" -> Messages("db.read.error")))
        }
      case None => Future.successful(Ok(views.html.users.signIn(Authentication.signInForm)))
    }
  }
*/

