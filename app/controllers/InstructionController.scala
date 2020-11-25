package controllers

import akka.stream.Materializer
import controllers.actions._
import javax.inject.{Inject, Singleton}
import models.formdata.{InstructionForm, InstructionPartForm}
import models.services._
import models.{DbModel, Instruction, InstructionPart, MediaTypes}
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{AbstractController, ControllerComponents}
import viewdata.{MediaData, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InstructionController @Inject() (
		generalActions: GeneralActions,
		userService: UserService,
		amlObjectService: AmlObjectService,
		domainService: DomainService,
		instructionService: InstructionService,
		fileService: FileService,
		components: ControllerComponents)(implicit exec: ExecutionContext, materialize: Materializer)
		extends AbstractController(components) with I18nSupport {
	implicit val lang: Lang = components.langs.availables.head

	def list(domainUuid: String, page: Int) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain).async {
		implicit domainRequest =>

		val responses = for {
			domainOpt <- {
				if (domainUuid.isEmpty()) {
					Future.successful(None)
				} else {
					domainService.findOneDomain(DbModel.queryByUuid(domainUuid))
				}
			}
			instructionListData <- instructionService.getInstructionList(page)
			objectChainList <- domainService.getAmlObjectDatas(instructionListData.list)
		} yield Ok(views.html.instructions.list(domainOpt, instructionListData, objectChainList,
				UserStatus(Some(domainRequest.identity), domainRequest.activeDomain)))

				responses recover {
				case e => InternalServerError(e.getMessage())
		}
	}

	def create(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
			generalActions.AmlObjectAction(amlObjectUuid)) {
		implicit amlObjectRequest =>
		Ok(views.html.browse.createInstruction(InstructionForm.form, AmlObjectData(amlObjectRequest.myDomain,
				amlObjectRequest.hierarchy, amlObjectRequest.elementChain ::: amlObjectRequest.interface.toList),
				UserStatus(Some(amlObjectRequest.identity), amlObjectRequest.activeDomain)))
	}

	def submitCreate(amlObjectUuid: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.AmlObjectAction(amlObjectUuid)).async {
		implicit amlObjectRequest =>
		InstructionForm.form.bindFromRequest().fold(
				formWithErrors => {
					Future.successful(BadRequest(views.html.browse.createInstruction(formWithErrors, AmlObjectData(amlObjectRequest.myDomain,
							amlObjectRequest.hierarchy, amlObjectRequest.elementChain ::: amlObjectRequest.interface.toList),
							UserStatus(Some(amlObjectRequest.identity), amlObjectRequest.activeDomain))))
				},
				formData => {
					val responses = for {
						optSavedInstruction <- instructionService.insertInstruction(Instruction.create(name = formData.name,
								connectionToToDomain = amlObjectRequest.myDomain.uuid,
								parentAmlObject = amlObjectRequest.elementOrInterfaceUuid,
								createdBy = amlObjectRequest.identity.uuid))
					} yield optSavedInstruction match {
					case Some(newInstruction) =>
					//TODO: Change to edit when exist
					Redirect(routes.InstructionController.instruction(newInstruction.uuid, 1)).
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
				InstructionForm.form.fill(myInstructionRequest.instruction), UserStatus(Some(myInstructionRequest.identity),
						myInstructionRequest.activeDomain)))
	}

	def submitEdit(uuid: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionAction(uuid)).async {
		implicit myInstructionRequest =>
		InstructionForm.form.bindFromRequest().fold(
				formWithErrors => {
					Future.successful(BadRequest(views.html.instructions.edit(myInstructionRequest.instruction, formWithErrors,
							UserStatus(Some(myInstructionRequest.identity), myInstructionRequest.activeDomain))))
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
          instructionPartsListData <- instructionService.getInstructionPartList(instructionRequest.instruction, page)
        } yield Ok(views.html.instructions.instruction(instructionRequest.instruction, AmlObjectData(instructionRequest.myDomain,
          instructionRequest.hierarchy, instructionRequest.elementChain ::: instructionRequest.interface.toList),
          instructionPartsListData, UserStatus(Some(instructionRequest.identity), instructionRequest.activeDomain)))

        responses recover {
          case e => InternalServerError(e.getMessage())
        }
    }

	def showPart(uuid: String, page: Int, mediaType: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
		implicit instructionPartRequest =>

		val responses = for {
			instructionOpt <- instructionService.
			  findOneInstruction(DbModel.queryByUuid(instructionPartRequest.instructionPart.parent))
			mediaExists <- MediaTypes.fromString(mediaType) match {
			case MediaTypes.MediaImage => fileService.imageExists(uuid)
			case MediaTypes.MediaVideo => fileService.videoExists(uuid)
			}
		} yield instructionOpt.map(instruction => Ok(views.html.instructions.showPart(
				instructionPartRequest.instructionPart, instruction,
				AmlObjectData(instructionPartRequest.myDomain, instructionPartRequest.hierarchy,
						instructionPartRequest.elementChain ::: instructionPartRequest.interface.toList), page,
						MediaData(MediaTypes.fromString(mediaType), mediaExists),
						UserStatus(Some(instructionPartRequest.identity), instructionPartRequest.activeDomain)))).
						  getOrElse(NotFound)

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
							AmlObjectData(instructionRequest.myDomain, instructionRequest.hierarchy,
						instructionRequest.elementChain ::: instructionRequest.interface.toList), formWithErrors, UserStatus(Some(instructionRequest.identity), instructionRequest.activeDomain))))
				},
				formData => {
					val responses = for {
						nextOrderNumber <- instructionService.getNextOrderNumber(instructionRequest.instruction)
						optSavedInstructionPart <- instructionService.insertInstructionPart(InstructionPart.create(
								orderNumber = nextOrderNumber,
								parentInstruction = instructionUuid,
								text = formData.text,
								shortText = formData.shortText,
								createdBy = instructionRequest.identity.uuid))
					} yield optSavedInstructionPart match {
					case Some(newInstructionPart) =>
					//TODO: Change to edit when exist
					Redirect(routes.InstructionController.showPart(newInstructionPart.uuid, 1, MediaTypes.MediaImage.stringValue)).
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

		Ok(views.html.instructions.createPart(instructionRequest.instruction, AmlObjectData(instructionRequest.myDomain, 
		    instructionRequest.hierarchy, instructionRequest.elementChain ::: instructionRequest.interface.toList),
		    InstructionPartForm.form, UserStatus(Some(instructionRequest.identity), instructionRequest.activeDomain)))
	}

	def submitEditPart(uuid: String, page: Int, mediaType: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
		implicit instructionPartRequest =>

		InstructionPartForm.form.bindFromRequest().fold(
				formWithErrors => {

					val responses = for {
						instructionOpt <- instructionService.findOneInstruction(DbModel.queryByUuid(instructionPartRequest.instructionPart.parent))
						instructionPartsListData <- instructionService.getInstructionPartList(instructionPartRequest.instruction, page)
						mediaExists <- MediaTypes.fromString(mediaType) match {
			        case MediaTypes.MediaImage => fileService.imageExists(uuid)
			        case MediaTypes.MediaVideo => fileService.videoExists(uuid)
			      }
					} yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart( 
					    instructionPartRequest.instructionPart, instruction, 
					    PaginateData(page, instructionPartsListData.list.size),
							AmlObjectData(instructionPartRequest.myDomain, instructionPartRequest.hierarchy,
						  instructionPartRequest.elementChain ::: instructionPartRequest.interface.toList), 
						  MediaData(MediaTypes.fromString(mediaType), mediaExists),
							formWithErrors, UserStatus(Some(instructionPartRequest.identity),
							instructionPartRequest.activeDomain)))).getOrElse(NotFound)

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
					Redirect(routes.InstructionController.editPart(uuid, page, mediaType)).
					  flashing("success" -> Messages("db.success.update"))
					case false =>
					Redirect(routes.InstructionController.editPart(uuid, page, mediaType)).
					  flashing("failure" -> Messages("db.failure.update"))
					}

					responses recover {
					case e => InternalServerError(e.getMessage())
					}
				})

	}

	def editPart(uuid: String, page: Int, mediaType: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
		implicit instructionPartRequest =>

		val responses = for {
			instructionOpt <- instructionService.findOneInstruction(DbModel.queryByUuid(instructionPartRequest.instructionPart.parent))
			instructionPartsListData <- instructionService.getInstructionPartList(instructionPartRequest.instruction, page)
			mediaExists <- MediaTypes.fromString(mediaType) match {
        case MediaTypes.MediaImage => fileService.imageExists(uuid)
			  case MediaTypes.MediaVideo => fileService.videoExists(uuid)
			}
		} yield instructionOpt.map(instruction => Ok(views.html.instructions.editPart(
		    instructionPartRequest.instructionPart, instruction, 
		    PaginateData(page, instructionPartsListData.list.size),
		    AmlObjectData(instructionPartRequest.myDomain, instructionPartRequest.hierarchy,
				instructionPartRequest.elementChain ::: instructionPartRequest.interface.toList), 
				MediaData(MediaTypes.fromString(mediaType), mediaExists),
				InstructionPartForm.form.fill(instructionPartRequest.instructionPart), UserStatus(Some(instructionPartRequest.identity),
				instructionPartRequest.activeDomain)))).getOrElse(NotFound)

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

	def movePartUp(uuid: String, page: Int, mediaTypeString: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
		implicit instructionPartRequest =>

		val responses = for {
			result <- instructionService.movePartUp(instructionPartRequest.instructionPart)
		} yield result match {
		case true => Redirect(routes.InstructionController.editPart(instructionPartRequest.instructionPart.uuid,
				page, mediaTypeString)).flashing("success" -> Messages("moveUp"))
		case false => Redirect(routes.InstructionController.editPart(instructionPartRequest.instructionPart.uuid,
				page, mediaTypeString)).flashing("error" -> Messages("moveUp"))
		}

		responses recover {
		case e => InternalServerError(e.getMessage())
		}
	}

	def movePartDown(uuid: String, page: Int, mediaTypeString: String) = (generalActions.MySecuredAction andThen
			generalActions.RequireActiveDomain andThen generalActions.InstructionPartAction(uuid)).async {
		implicit instructionPartRequest =>

		val responses = for {
			result <- instructionService.movePartDown(instructionPartRequest.instructionPart)
		} yield result match {
		case true => Redirect(routes.InstructionController.editPart(instructionPartRequest.instructionPart.uuid,
				page, mediaTypeString)).flashing("success" -> Messages("moveDown"))
		case false => Redirect(routes.InstructionController.editPart(instructionPartRequest.instructionPart.uuid,
				page, mediaTypeString)).flashing("error" -> Messages("moveDown"))
		}

		responses recover {
		case e => InternalServerError(e.getMessage())
		}
	}
}

