package controllers

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import akka.stream.Materializer
import models.formdata.OrganisationForm
import models.formdata.OrganisationForm.fromOrganisationToData
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
import models.formdata.FactoryForm
import models.Factory
import models.services.InstructionService
import models.formdata.InstructionForm
import models.Instruction
import models.services.AmlObjectService
import models.formdata.InstructionPartForm
import models.InstructionPart
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import models.AmlObject

@Singleton
class InstructionController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val userService: UserService,
  val organisationService: OrganisationService,
  val amlObjectService: AmlObjectService,
  val factoryService: FactoryService,
  val instructionService: InstructionService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(factoryUuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation).async {
      implicit factoryRequest =>

        val responses = for {
          factoryOpt <- {
            if (factoryUuid.isEmpty()) {
              Future.successful(None)
            } else {
              factoryService.findOneFactory(Factory.queryByUuid(factoryUuid))
            }
          }
          instructionListData <- instructionService.getInstructionList(page)
          objectChainList <- factoryService.getAmlObjectChains(instructionListData.list)
        } yield Ok(views.html.instructions.list(factoryOpt, instructionListData.list, objectChainList,
          instructionListData.paginateData, Some(factoryRequest.identity), factoryRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def create(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation) {
      implicit mySecuredRequest =>
        Ok(views.html.instructions.create(InstructionForm.form, amlObjectUuid, Some(mySecuredRequest.identity),
          mySecuredRequest.activeOrganisation))
    }

  def submitCreate(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.AmlObjectAction(amlObjectUuid)).async {
      implicit amlObjectRequest =>
        InstructionForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.create(formWithErrors, amlObjectUuid,
              Some(amlObjectRequest.identity), amlObjectRequest.activeOrganisation)))
          },
          formData => {
            val responses = for {
              optSavedInstruction <- instructionService.insertInstruction(Instruction.create(name = formData.name,
                connectionToToFactory = amlObjectRequest.elementOrInterface.fold(_.connectionTo, _.connectionTo),
                parentAmlObject = amlObjectRequest.elementOrInterface.fold(_.uuid, _.uuid),
                createdBy = amlObjectRequest.identity.uuid))
            } yield optSavedInstruction match {
              case Some(newInstruction) =>
                //TODO: Change to edit when exist
                Redirect(routes.InstructionController.list("", 1)).
                  flashing("success" -> Messages("db.success.insert", newInstruction.name))
              case None =>
                Redirect(routes.InstructionController.create(amlObjectUuid)).
                  flashing("failure" -> Messages("db.failure.insert", formData.name))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })
    }

  def edit(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(uuid)) {
      implicit myInstructionRequest =>
        Ok(views.html.instructions.edit(myInstructionRequest.instruction,
          InstructionForm.form.fill(myInstructionRequest.instruction), Some(myInstructionRequest.identity),
          myInstructionRequest.activeOrganisation))
    }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(uuid)).async {
      implicit myInstructionRequest =>
        InstructionForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.edit(myInstructionRequest.instruction, formWithErrors,
              Some(myInstructionRequest.identity), myInstructionRequest.activeOrganisation)))
          },
          formData => {
            val responses = for {
              optUpdatedInstruction <- instructionService.updateInstruction(myInstructionRequest.
                instruction.copy(name = formData.name))
            } yield optUpdatedInstruction match {
              case true =>
                //TODO: Change to edit when exist
                Redirect(routes.InstructionController.list("", 1)).
                  flashing("success" -> Messages("db.success.update", formData.name))
              case false =>
                Redirect(routes.InstructionController.edit(uuid)).
                  flashing("failure" -> Messages("db.failure.update", myInstructionRequest.instruction.name))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })
    }

  def delete(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(uuid)).async {
      implicit instructionRequest =>

        val responses = for {
          removeResult <- instructionService.removeInstruction(instructionRequest.instruction)
        } yield if (removeResult.success) {
          Redirect(routes.InstructionController.list("", 1)).
            flashing("success" -> Messages("db.success.remove", instructionRequest.instruction.name))
        } else {
          Redirect(routes.InstructionController.list("", 1)).
            flashing("error" -> removeResult.getReason)
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def instruction(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(uuid)).async {
      implicit instructionRequest =>

        val responses = for {
          factoryOpt <- factoryService.findOneFactory(Factory.queryByUuid(instructionRequest.instruction.connectionTo))
          amlObjectOpt <- amlObjectService.findOneElementOrInterface(AmlObject.queryByUuid(instructionRequest.instruction.parent))
          instructionPartsListData <- instructionService.getInstructionPartList(instructionRequest.instruction, page)
        } yield Ok(views.html.instructions.instruction(instructionRequest.instruction, amlObjectOpt.get, factoryOpt.get,
          instructionPartsListData.list, instructionPartsListData.paginateData, Some(instructionRequest.identity),
          instructionRequest.activeOrganisation))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def inspectPart(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
        } yield instructionOpt.map(instruction => Ok(views.html.instructions.inspectInstructionPart(
          instructionPartRequest.instructionPart, instruction, page,
          Some(instructionPartRequest.identity), instructionPartRequest.activeOrganisation))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }

    }

  def submitCreatePart(instructionUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(instructionUuid)).async {
      implicit instructionRequest =>

        InstructionPartForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.createPart(instructionRequest.instruction,
              formWithErrors, Some(instructionRequest.identity), instructionRequest.activeOrganisation)))
          },
          formData => {
            val responses = for {
              nextOrderNumber <- instructionService.getNextOrderNumber(instructionRequest.instruction)
              optSavedInstructionPart <- instructionService.insertInstructionPart(InstructionPart.create(
                orderNumber = nextOrderNumber,
                parentInstruction = instructionUuid,
                text = formData.text,
                createdBy = instructionRequest.identity.uuid))
            } yield optSavedInstructionPart match {
              case Some(newInstructionPart) =>
                //TODO: Change to edit when exist
                Redirect(routes.InstructionController.instruction(instructionUuid, 1)).
                  flashing("success" -> Messages("db.success.insert", newInstructionPart.uuid))
              case None =>
                Redirect(routes.InstructionController.createPart(instructionUuid)).
                  flashing("failure" -> Messages("db.failure.insert"))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })
    }

  def createPart(instructionUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionAction(instructionUuid)) {
      implicit instructionRequest =>

        Ok(views.html.instructions.createPart(instructionRequest.instruction, InstructionPartForm.form,
          Some(instructionRequest.identity), instructionRequest.activeOrganisation))
    }

  def submitEditPart(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        InstructionPartForm.form.bindFromRequest().fold(
          formWithErrors => {

            val responses = for {
              instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
            } yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart(instruction,
              instructionPartRequest.instructionPart,
              formWithErrors, Some(instructionPartRequest.identity),
              instructionPartRequest.activeOrganisation))).getOrElse(NotFound)

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          },
          formData => {
            val responses = for {
              updateResult <- {
                val updateinstructionPart = instructionPartRequest.instructionPart.copy(text = formData.text)
                instructionService.updateInstructionPart(updateinstructionPart)
              }
            } yield updateResult match {
              case true =>
                Redirect(routes.InstructionController.editPart(uuid)).
                  flashing("success" -> Messages("db.success.update"))
              case false =>
                Redirect(routes.InstructionController.editPart(uuid)).
                  flashing("failure" -> Messages("db.failure.update"))
            }

            responses recover {
              case e => InternalServerError(e.getMessage())
            }
          })

    }

  def editPart(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
        } yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart(instruction,
          instructionPartRequest.instructionPart,
          InstructionPartForm.form.fill(instructionPartRequest.instructionPart), Some(instructionPartRequest.identity),
          instructionPartRequest.activeOrganisation))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def deletePart(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val instructionUuid = instructionPartRequest.instructionPart.parent
        val responses = for {
          removeResult <- instructionService.removeInstructionPart(instructionPartRequest.instructionPart)
        } yield if (removeResult.success) {
          Redirect(routes.InstructionController.instruction(instructionUuid, 1)).
            flashing("success" -> Messages("db.success.remove", instructionPartRequest.instructionPart.uuid))
        } else {
          Redirect(routes.InstructionController.instruction(instructionUuid, 1)).
            flashing("error" -> removeResult.getReason)
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def movePartUp(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          result <- instructionService.movePartUp(instructionPartRequest.instructionPart)
        } yield result match {
          case true => Redirect(routes.InstructionController.instruction(instructionPartRequest.instructionPart.parent,
            page)).flashing("success" -> Messages("moveUp"))
          case false => Redirect(routes.InstructionController.instruction(instructionPartRequest.instructionPart.parent,
            page)).flashing("error" -> Messages("moveUp"))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def movePartDown(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveOrganisation andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          result <- instructionService.movePartDown(instructionPartRequest.instructionPart)
        } yield result match {
          case true => Redirect(routes.InstructionController.instruction(instructionPartRequest.instructionPart.parent,
            page)).flashing("success" -> Messages("moveDown"))
          case false => Redirect(routes.InstructionController.instruction(instructionPartRequest.instructionPart.parent,
            page)).flashing("error" -> Messages("moveDown"))
        }

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }
}
/*
  def listByElement(factoryIdString: String, elementIdString: String, page: Int) =
    SecuredAction(AlwaysAuthorized()).async { implicit request =>
    request.identity.activeOrganisation match {
      case Some(activeOrganisationObjectId) => {

        val cursor = InstructionDAO.find(Instruction.refersToElementKey $eq (new ObjectId(elementIdString))).
          sort(DbHelper.sortAscKey(Instruction.titleKey))

        val count = cursor.count
        val instructions = DbHelper.paginate(cursor, page, models.defaultPageLength).toList

        Logger.info("InstructionController listByElement, instructions: " + instructions)

        Logger.info("InstructionController listByElement, count: " + count)

        Future.successful(Ok(views.html.instructions.list(instructions, Some(factoryIdString), Some(elementIdString),
          count, page, models.defaultPageLength, Some(request.identity))))
      }
      case None => Future.successful(Ok(views.html.instructions.list(List(), Some(factoryIdString),
        Some(elementIdString), 0, page, models.defaultPageLength, Some(request.identity))))
    }
  }

  def create(factoryIdString: String, elementIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Future.successful(Ok(views.html.instructions.create(Instruction.instructionForm, factoryIdString, elementIdString, Some(request.identity))))
  }

  def editInstructionEdit(instructionIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Future.successful(Ok(views.html.instructions.editInstruction(
      Instruction.instructionForm.fill(InstructionDAO.findOneById(instructionIdString).get), Some(request.identity))))
  }

  def save(factoryIdString: String, elementIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("InstructionController save")
    Instruction.instructionForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.info("formWithErrors:" + formWithErrors.toString)
        Future.successful(BadRequest(views.html.instructions.create(formWithErrors, factoryIdString, elementIdString, Some(request.identity))))
      },
      newInstruction => {
        Logger.info("newInstruction: " + newInstruction)
        //Add the new instruction
        InstructionDAO.insert(newInstruction) match {
          case Some(objectId) => Future.successful(Redirect(routes.InstructionController.
            addInstructionPartEdit(newInstruction._id.toString, 1)).flashing("success" -> Messages("db.success.save",
            newInstruction.title)))
          case None => Future.successful(Redirect(routes.InstructionController.list(1)).
            flashing("error" -> Messages("db.error.write")))
        }
      }
    )
  }

  def editInstructionUpdate = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("InstructionController editInstructionPart")
    Instruction.instructionForm.bindFromRequest().fold(
      formWithErrors => {
        Logger.info("formWithErrors:" + formWithErrors.toString)
        Future.successful(BadRequest(views.html.instructions.editInstruction(formWithErrors, Some(request.identity))))
      },
      updatedInstruction => {
        Logger.info("updatedInstruction: " + updatedInstruction)
        //Add the new instruction
        InstructionDAO.update(InstructionParams(_id = Some(updatedInstruction._id)), updatedInstruction)
        Future.successful(Redirect(routes.InstructionController.addInstructionPartEdit(updatedInstruction._id.toString, 1)).
          flashing("success" -> Messages("db.success.update", updatedInstruction.title)))
      }
    )
  }

  def showInstruction(instructionIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController showInstruction")

      request.identity.activeOrganisation match {
        case None => {
          Future.successful(Redirect(routes.OrganisationController.list(1)).
            flashing("error" -> Messages("select.active.organisation")))
        }
        case Some(organisationId) => {
          val instruction = InstructionDAO.findOneById(instructionIdString).get

          val allowedUsers = User.getAllowedUsers(organisationId)

          val instructionPartsCursor = InstructionPartDAO.find(InstructionPart.instructionKey $eq instruction._id).
            sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey))

          val count = instructionPartsCursor.count
          val instructionParts = DbHelper.paginate(instructionPartsCursor, page, models.defaultPageLength).toList

          Future.successful(Ok(views.html.instructions.showInstruction(instruction, instructionParts, count, page,
            models.defaultPageLength, allowedUsers, Some(request.identity))))
        }
      }
  }

  def deleteInstruction(instructionIdString: String) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController deleteInstruction")

      request.identity.activeOrganisation match {
        case None => {
          Future.successful(Redirect(routes.OrganisationController.list(1)).
            flashing("error" -> Messages("select.active.organisation")))
        }
        case Some(organisationId) => {

          InstructionPartDAO.remove(InstructionPart.instructionKey $eq new ObjectId(instructionIdString))

          InstructionDAO.removeById(instructionIdString)

          Future.successful(Redirect(routes.InstructionController.list(1)))
        }
      }
  }

  def addInstructionPartEdit(instructionIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController addInstructionPartEdit")

    request.identity.activeOrganisation match {
      case None => {
        Future.successful(Redirect(routes.OrganisationController.list(1)).
          flashing("error" -> Messages("select.active.organisation")))
      }
      case Some(organisationId) => {
        val instruction = InstructionDAO.findOneById(instructionIdString).get

        val allowedUsers = User.getAllowedUsers(organisationId)

        val instructionPartsCursor = InstructionPartDAO.find(InstructionPart.instructionKey $eq instruction._id).
          sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey))

        val count = instructionPartsCursor.count
        val instructionParts = DbHelper.paginate(instructionPartsCursor, page, models.defaultPageLength).toList

        Future.successful(Ok(views.html.instructions.addInstructionPart(InstructionPart.instructionPartForm,
          instruction, instructionParts, count, page, models.defaultPageLength, allowedUsers, Some(request.identity))))
      }
    }
  }

  def addInstructionPartUpdate(instructionIdString: String, page: Int) =
    SecuredAction(AlwaysAuthorized()).async(parse.multipartFormData) { implicit request =>
      Logger.info("InstructionController addInstructionPartUpdate")

      InstructionPart.instructionPartForm.bindFromRequest().fold(
        formWithErrors => {
          Logger.info("formWithErrors")
          //TODO: No check on get, fix
          request.identity.activeOrganisation match {
            case None => {
              Future.successful(Redirect(routes.OrganisationController.list(1)).
                flashing("error" -> Messages("select.active.organisation")))
            }
            case Some(organisationId) => {
              val instruction = InstructionDAO.findOneById(instructionIdString).get

              val allowedUsers = User.getAllowedUsers(organisationId)

              val instructionPartsCursor = InstructionPartDAO.find(InstructionPart.instructionKey $eq instruction._id).
                sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey))

              val count = instructionPartsCursor.count
              val instructionParts = DbHelper.paginate(instructionPartsCursor, page, models.defaultPageLength).toList

              Future.successful(BadRequest(views.html.instructions.addInstructionPart(formWithErrors, instruction,
                instructionParts, count, page, models.defaultPageLength, allowedUsers, Some(request.identity))))
            }
          }
        },
        newInstructionPart => {

          Logger.info("newInstructionPart: " + newInstructionPart.toString)

          val newInstructionPartWithMedia = InstructionPart.
            saveMediaFile(newInstructionPart, request.body.file(models.imageFileKeyString))

          //Add the instruction update to the instruction
          InstructionDAO.findOneById(instructionIdString) match {
            case Some(instruction) =>

              //It's a new instructionPart, insert it
              InstructionPartDAO.insert(newInstructionPartWithMedia)

            case None => //This is some serious wrong
          }

          Future.successful(Redirect(routes.InstructionController.addInstructionPartEdit(instructionIdString, page)).
            flashing("success" -> Messages("db.success.update", InstructionDAO.findOneById(instructionIdString).get.title)))
        }
      )
    }

  def ipMoveUp(instructionPartIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController ipMoveUp")

      val instructionPartToMoveUp = InstructionPartDAO.findOneById(instructionPartIdString).get
      val instructionPartToMoveDown = InstructionPartDAO.
        findOne(DBObject(InstructionPart.instructionKey -> instructionPartToMoveUp.instruction,
        NumericlyOrdered.orderNumberKey -> (instructionPartToMoveUp.orderNumber - 1))).get

        Logger.info("InstructionController ipMoveUp, instructionPartToMoveUp: " + instructionPartToMoveUp)
        Logger.info("InstructionController ipMoveUp, instructionPartToMoveDown: " + instructionPartToMoveDown )

          InstructionPartDAO.update(InstructionPartParams(_id = Some(instructionPartToMoveUp._id)),
            instructionPartToMoveUp.copy(orderNumber = instructionPartToMoveUp.orderNumber - 1))
          InstructionPartDAO.update(InstructionPartParams(_id = Some(instructionPartToMoveDown._id)),
            instructionPartToMoveDown.copy(orderNumber = instructionPartToMoveDown.orderNumber + 1))

      Future.successful(Redirect(routes.InstructionController.
        addInstructionPartEdit(instructionPartToMoveUp.instruction.toString, page)))
  }

  def ipMoveDown(instructionPartIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController ipMoveDown")

      val instructionPartToMoveDown = InstructionPartDAO.findOneById(instructionPartIdString).get
      val instructionPartToMoveUp = InstructionPartDAO.
        findOne(DBObject(InstructionPart.instructionKey -> instructionPartToMoveDown.instruction,
        NumericlyOrdered.orderNumberKey -> (instructionPartToMoveDown.orderNumber + 1))).get

      Logger.info("InstructionController ipMoveDown, instructionPartToMoveDown: " + instructionPartToMoveDown )
      Logger.info("InstructionController ipMoveDown, instructionPartToMoveUp: " + instructionPartToMoveUp)

      InstructionPartDAO.update(InstructionPartParams(_id = Some(instructionPartToMoveUp._id)),
        instructionPartToMoveUp.copy(orderNumber = instructionPartToMoveUp.orderNumber - 1))
      InstructionPartDAO.update(InstructionPartParams(_id = Some(instructionPartToMoveDown._id)),
        instructionPartToMoveDown.copy(orderNumber = instructionPartToMoveDown.orderNumber + 1))

      Future.successful(Redirect(routes.InstructionController.
        addInstructionPartEdit(instructionPartToMoveUp.instruction.toString, page)))
  }

  def ipDelete(instructionPartIdString: String, page: Int) = SecuredAction(AlwaysAuthorized()).async {
    implicit request =>

      Logger.info("InstructionController ipDelete")

      val instructionObjectId: ObjectId = InstructionPartDAO.findOneById(instructionPartIdString).map(_.instruction).getOrElse(new ObjectId)

      val orderNumberRemoved = InstructionPartDAO.findOneById(instructionPartIdString).fold(1)(_.orderNumber)

      Logger.info("InstructionController orderNumberRemoved = " + orderNumberRemoved)

      InstructionPartDAO.removeById(instructionPartIdString)

      for(instructionPart <- InstructionPartDAO.find(DBObject(InstructionPart.instructionKey -> instructionObjectId) ++
        (NumericlyOrdered.orderNumberKey $gt orderNumberRemoved)).toList) {

        InstructionPartDAO.update(InstructionPartParams(_id = Some(instructionPart._id)),
          instructionPart.copy(orderNumber = instructionPart.orderNumber - 1))
      }

      Future.successful(Redirect(routes.InstructionController.addInstructionPartEdit(instructionObjectId.toString, page)))
  }

  def image(instructionPartIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("imageFile, instructionPartIdString: " + instructionPartIdString)
    InstructionPartDAO.findOneById(instructionPartIdString) match {
      case Some(instructionPart) => {
        InstructionPart.getImageFile(instructionPart) match {
          case Some(imageFile) => {
            Future.successful(Ok.sendFile(
              content = imageFile,
              inline = true
            ))
          }
          case None =>
            Future.successful(Redirect(routes.InstructionController.list(1)).
              flashing("error" -> Messages("db.error.read.file", instructionPartIdString)))
        }
      }
      case None => Future.successful(Redirect(routes.FactoryController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def thumbnail(instructionPartIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("imageFile, instructionPartIdString: " + instructionPartIdString)
    InstructionPartDAO.findOneById(instructionPartIdString) match {
      case Some(instructionPart) => {
        InstructionPart.getThumbnailFile(instructionPart) match {
          case Some(thumbnailFile) => {
            Future.successful(Ok.sendFile(
              content = thumbnailFile,
              inline = true
            ))
          }
          case None =>
            Future.successful(Redirect(routes.InstructionController.list(1)).
              flashing("error" -> Messages("db.error.read.file", instructionPartIdString)))
        }
      }
      case None => Future.successful(Redirect(routes.FactoryController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def video(instructionPartIdString: String) = SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("imageFile, instructionPartIdString: " + instructionPartIdString)
    InstructionPartDAO.findOneById(instructionPartIdString) match {
      case Some(instructionPart) => {
        InstructionPart.getVideoFile(instructionPart) match {
          case Some(videoFile) => {
            Future.successful(Ok.sendFile(
              content = videoFile,
              inline = true
            ))
          }
          case None =>
            Future.successful(Redirect(routes.InstructionController.list(1)).
              flashing("error" -> Messages("db.error.read.file", instructionPartIdString)))
        }
      }
      case None => Future.successful(Redirect(routes.FactoryController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }
}*/
