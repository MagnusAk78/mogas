package models

import java.util.UUID
import play.api.libs.json.Json

case class Instruction(
  override val uuid: String,
  override val modelType: String,
  override val name: String,
  override val connectionTo: String,
  override val parent: String,
  override val createdBy: String) extends DbModel 
    with HasModelType with HasName with HasConnectionTo
    with HasParent 
    with HasCreatedBy {
}

object Instruction {

  implicit val instructionFormat = Json.format[Instruction]

  def create(name: String, connectionToToDomain: String, parentAmlObject: String, createdBy: String) =
    Instruction(uuid = UUID.randomUUID.toString, modelType=Types.InstructionType.stringValue,
        name = name, connectionTo = connectionToToDomain, parent = parentAmlObject, createdBy = createdBy)
}

