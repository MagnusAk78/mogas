package models

import java.util.UUID
import play.api.libs.json._

case class InstructionPart(
  override val uuid: String,
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String,
  val shortText: String) extends DbModel with OrderedModel with ChildOf[Instruction] with CreatedBy
    with HasText {

  override def asJsObject: JsObject = {
    InstructionPart.orderedModelJsObject(this) ++
      InstructionPart.childOfJsObject(this) ++
      InstructionPart.createdByJsObject(this) ++
      InstructionPart.hasTextJsObject(this) ++
      Json.obj(InstructionPart.KeyShortText -> JsString(shortText))
  }
}

object InstructionPart extends DbModelComp[InstructionPart] with ChildOfComp[Instruction] with CreatedByComp
    with HasTextComp with OrderedModelComp {
  implicit val instructionPartFormat = Json.format[InstructionPart]

  private val KeyShortText = "shortText"

  def create(orderNumber: Int, parentInstruction: String, text: String, createdBy: String, shortText: String) =
    InstructionPart(uuid = UUID.randomUUID.toString, orderNumber = orderNumber, parent = parentInstruction,
      createdBy = createdBy, text = text, shortText = shortText)
}
