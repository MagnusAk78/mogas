package models

import java.util.UUID
import play.api.libs.json._

case class Instruction(
  override val uuid: String,
  override val name: String,
  override val connectionTo: String,
  override val parent: String,
  override val createdBy: String) extends DbModel with NamedModel with ConnectionTo[Factory] with ChildOf[AmlObject]
    with CreatedBy {

  override def asJsObject: JsObject = {
    Instruction.connectionToJsObject(this) ++ Instruction.namedModelJsObject(this) ++
      Instruction.childOfJsObject(this) ++ Instruction.createdByJsObject(this)
  }
}

object Instruction extends DbModelComp[Instruction] with ChildOfComp[AmlObject] with ConnectionToComp[Factory]
    with CreatedByComp with NamedModelComp {
  implicit val instructionFormat = Json.format[Instruction]

  private val KeyCreatedByUser = "createdByUser"

  def create(name: String, connectionToToFactory: String, refersToAmlObject: String, createdBy: String) =
    Instruction(uuid = UUID.randomUUID.toString, name = name, connectionTo = connectionToToFactory,
      parent = refersToAmlObject, createdBy = createdBy)
}

/**
 *
 * object Instruction {
 *
 * val instructionPartsKey = "instructionParts"
 *
 * val titleKey = "title"
 *
 * val factoryKey = "factory"
 *
 * val refersToElementKey = "refersToElement"
 *
 * /**
 * Forms and validation
 * */
 *
 * val instructionForm: Form[Instruction] = {
 * Form(mapping("id" -> optional(nonEmptyText).verifying((optionalIdString: Option[String]) => optionalIdString match {
 * case Some(objectIdString) => {
 * ObjectId.isValid(objectIdString)
 * }
 * case None => true
 * }),
 * "factory" -> nonEmptyText.verifying(FactoryDAO.findOneById(_).nonEmpty),
 * "refersToElement" -> nonEmptyText,
 * "createdBy" -> nonEmptyText.verifying(UserDAO.findOneById(_).nonEmpty),
 * "title" -> nonEmptyText)
 * (Instruction.instructionFormApply)(Instruction.instructionFormUnapply))
 * }
 *
 * def instructionFormApply(optionalIdString: Option[String], factoryIdString: String, factoryElementIdString: String,
 * createdByIdString: String, title: String): Instruction = {
 *
 * val objectId = optionalIdString match {
 * case Some(idString) => new ObjectId(idString)
 * case None => new ObjectId()
 * }
 *
 * val existingInstruction = InstructionDAO.findOneById(objectId)
 *
 * val instruction = Instruction(_id = objectId, factory = new ObjectId(factoryIdString),
 * refersToElement = new ObjectId(factoryElementIdString), createdBy = new ObjectId(createdByIdString),
 * title = title)
 *
 * Logger.info("instructionFormApply instruction: " + instruction.toString)
 *
 * instruction
 * }
 *
 * def instructionFormUnapply(instruction: Instruction) = {
 * Logger.info("instructionFormUnapply instruction: " + instruction.toString)
 * Some(Some(instruction._id.toString), instruction.factory.toString, instruction.refersToElement.toString,
 * instruction.createdBy.toString, instruction.title)
 * }
 * }
 */
