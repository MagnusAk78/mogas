package models

import java.util.UUID
import play.api.libs.json.Json

case class InstructionPart(
  override val uuid: String,
  override val modelType: String, 
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String,
  override val shortText: String) extends DbModel with HasModelType with OrderedModel with HasParent with HasCreatedBy
    with HasText with HasShortText {
}

object InstructionPart {

  implicit val instructionPartFormat = Json.format[InstructionPart]

  def create(orderNumber: Int, parentInstruction: String, text: String, createdBy: String, shortText: String) =
    InstructionPart(uuid = UUID.randomUUID.toString, modelType=Types.InstructionPartType.stringValue, 
        orderNumber = orderNumber, parent = parentInstruction, createdBy = createdBy, text = text, shortText = shortText)
}
