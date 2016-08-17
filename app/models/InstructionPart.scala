package models

import java.util.UUID
import play.api.libs.json._

case class InstructionPart(
  override val uuid: String,
  override val modelType: String, 
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String,
  override val shortText: String) extends DbModel with JsonImpl with HasModelType with OrderedModel with ChildOf[Instruction] with CreatedBy
    with HasText with HasShortText {

  override def asJsObject: JsObject = {
    InstructionPart.hasModelTypeJsObject(this) ++
    InstructionPart.hasShortTextJsObject(this) ++
    InstructionPart.orderedModelJsObject(this) ++
      InstructionPart.childOfJsObject(this) ++
      InstructionPart.createdByJsObject(this) ++
      InstructionPart.hasTextJsObject(this) ++
      InstructionPart.hasShortTextJsObject(this)
  }
}

object InstructionPart extends DbModelComp[InstructionPart] with HasModelTypeComp with ChildOfComp[Instruction] 
  with CreatedByComp with HasTextComp with OrderedModelComp with HasShortTextComp {
  implicit val instructionPartFormat = Json.format[InstructionPart]

  private val KeyShortText = "shortText"

  def create(orderNumber: Int, parentInstruction: String, text: String, createdBy: String, shortText: String) =
    InstructionPart(uuid = UUID.randomUUID.toString, modelType=Types.InstructionPartType.stringValue, 
        orderNumber = orderNumber, parent = parentInstruction, createdBy = createdBy, text = text, shortText = shortText)
}
