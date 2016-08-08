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
  val amlObjectService: AmlObjectService,
  val domainService: DomainService,
  val instructionService: InstructionService,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with I18nSupport {

  def list(domainUuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain).async {
      implicit domainRequest =>

        val responses = for {
          domainOpt <- {
            if (domainUuid.isEmpty()) {
              Future.successful(None)
            } else {
              domainService.findOneDomain(Domain.queryByUuid(domainUuid))
            }
          }
          instructionListData <- instructionService.getInstructionList(page)
          objectChainList <- domainService.getAmlObjectChains(instructionListData.list)
        } yield Ok(views.html.instructions.list(domainOpt, instructionListData.list, objectChainList,
          instructionListData.paginateData, Some(domainRequest.identity), domainRequest.activeDomain))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def create(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain) {
      implicit mySecuredRequest =>
        Ok(views.html.instructions.create(InstructionForm.form, amlObjectUuid, Some(mySecuredRequest.identity),
          mySecuredRequest.activeDomain))
    }

  def submitCreate(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.AmlObjectAction(amlObjectUuid)).async {
      implicit amlObjectRequest =>
        InstructionForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.create(formWithErrors, amlObjectUuid,
              Some(amlObjectRequest.identity), amlObjectRequest.activeDomain)))
          },
          formData => {
            val responses = for {
              optSavedInstruction <- instructionService.insertInstruction(Instruction.create(name = formData.name,
                connectionToToDomain = amlObjectRequest.elementOrInterface.fold(_.connectionTo, _.connectionTo),
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(uuid)) {
      implicit myInstructionRequest =>
        Ok(views.html.instructions.edit(myInstructionRequest.instruction,
          InstructionForm.form.fill(myInstructionRequest.instruction), Some(myInstructionRequest.identity),
          myInstructionRequest.activeDomain))
    }

  def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(uuid)).async {
      implicit myInstructionRequest =>
        InstructionForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.edit(myInstructionRequest.instruction, formWithErrors,
              Some(myInstructionRequest.identity), myInstructionRequest.activeDomain)))
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(uuid)).async {
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(uuid)).async {
      implicit instructionRequest =>

        val responses = for {
          domainOpt <- domainService.findOneDomain(Domain.queryByUuid(instructionRequest.instruction.connectionTo))
          amlObjectOpt <- amlObjectService.findOneElementOrInterface(AmlObject.queryByUuid(instructionRequest.instruction.parent))
          instructionPartsListData <- instructionService.getInstructionPartList(instructionRequest.instruction, page)
        } yield Ok(views.html.instructions.instruction(instructionRequest.instruction, amlObjectOpt.get, domainOpt.get,
          instructionPartsListData.list, instructionPartsListData.paginateData, Some(instructionRequest.identity),
          instructionRequest.activeDomain))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def inspectPart(uuid: String, page: Int) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
        } yield instructionOpt.map(instruction => Ok(views.html.instructions.inspectInstructionPart(
          instructionPartRequest.instructionPart, instruction, page,
          Some(instructionPartRequest.identity), instructionPartRequest.activeDomain))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }

    }

  def submitCreatePart(instructionUuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(instructionUuid)).async {
      implicit instructionRequest =>

        InstructionPartForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(views.html.instructions.createPart(instructionRequest.instruction,
              formWithErrors, Some(instructionRequest.identity), instructionRequest.activeDomain)))
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionAction(instructionUuid)) {
      implicit instructionRequest =>

        Ok(views.html.instructions.createPart(instructionRequest.instruction, InstructionPartForm.form,
          Some(instructionRequest.identity), instructionRequest.activeDomain))
    }

  def submitEditPart(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        InstructionPartForm.form.bindFromRequest().fold(
          formWithErrors => {

            val responses = for {
              instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
            } yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart(instruction,
              instructionPartRequest.instructionPart,
              formWithErrors, Some(instructionPartRequest.identity),
              instructionPartRequest.activeDomain))).getOrElse(NotFound)

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
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
      implicit instructionPartRequest =>

        val responses = for {
          instructionOpt <- instructionService.findOneInstruction(Instruction.queryByUuid(instructionPartRequest.instructionPart.parent))
        } yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart(instruction,
          instructionPartRequest.instructionPart,
          InstructionPartForm.form.fill(instructionPartRequest.instructionPart), Some(instructionPartRequest.identity),
          instructionPartRequest.activeDomain))).getOrElse(NotFound)

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

  def deletePart(uuid: String) = (generalActions.MySecuredAction andThen
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
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
    generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
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

