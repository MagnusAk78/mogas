package models

import java.util.UUID
import play.api.libs.json._

case class InstructionPart(
  override val uuid: String,
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String) extends DbModel with OrderedModel with ChildOf[Instruction] with CreatedBy
    with HasText {

  override def asJsObject: JsObject = {
    InstructionPart.orderedModelJsObject(this) ++
      InstructionPart.childOfJsObject(this) ++
      InstructionPart.createdByJsObject(this) ++
      InstructionPart.hasTextJsObject(this)
  }
}

object InstructionPart extends DbModelComp[InstructionPart] with ChildOfComp[Instruction] with CreatedByComp
    with HasTextComp with OrderedModelComp {
  implicit val instructionPartFormat = Json.format[InstructionPart]

  def create(orderNumber: Int, parentInstruction: String, text: String, createdBy: String) =
    InstructionPart(uuid = UUID.randomUUID.toString, orderNumber = orderNumber, parent = parentInstruction,
      createdBy = createdBy, text = text)
}

/**
 * object InstructionPart extends ImageHandler[InstructionPart] with VideoHandler[InstructionPart] {
 *
 * val instructionKey = "instruction"
 *
 * def saveMediaFile(instructionPart: InstructionPart,
 * filedata: Option[MultipartFormData.FilePart[Files.TemporaryFile]]): InstructionPart = filedata match {
 * case Some(uploadedFile) => {
 * val contentType = uploadedFile.contentType.getOrElse("")
 *
 * if (isImageContent(contentType)) {
 * val imageFileReps = saveImageFile(filedata)
 * instructionPart.copy(imageFileRep = imageFileReps._1, thumbnailFileRep = imageFileReps._2)
 * } else if (isVideoContent(contentType)) {
 * instructionPart.copy(videoFileRep = saveVideoFile(filedata))
 * } else {
 * instructionPart
 * }
 * }
 * case None => instructionPart
 * }
 *
 * /**
 * Forms and validation
 * */
 *
 * val instructionPartForm: Form[InstructionPart] = {
 * Form(mapping("id" -> optional(nonEmptyText).verifying((optionalIdString: Option[String]) => optionalIdString match {
 * case Some(objectIdString) => {
 * ObjectId.isValid(objectIdString)
 * }
 * case None => true
 * }),
 * "instructionId" -> nonEmptyText,
 * "createdBy" -> nonEmptyText,
 * "orderNumber" -> number,
 * "textDescription" -> nonEmptyText)
 * (instructionPartFormApply)(instructionPartFormUnapply))
 * }
 *
 * def instructionPartFormApply(optionalIdString: Option[String], instructionIdString: String, createdBy: String,
 * orderNumber: Int, textDescription: String): InstructionPart = {
 *
 * val objectId = optionalIdString match {
 * case Some(idString) => new ObjectId(idString)
 * case None => new ObjectId()
 * }
 *
 * InstructionPart(_id = objectId, instruction = instructionIdString, createdBy = new ObjectId(createdBy),
 * orderNumber = orderNumber, textDescription = textDescription, imageFileRep = None, thumbnailFileRep = None,
 * videoFileRep = None)
 * }
 *
 * def instructionPartFormUnapply(instructionPart: InstructionPart) = {
 * Some(Some(instructionPart._id.toString), instructionPart.instruction.toString, instructionPart.createdBy.toString,
 * instructionPart.orderNumber, instructionPart.textDescription)
 * }
 * }
 */
