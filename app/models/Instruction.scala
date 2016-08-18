package models

import java.util.UUID
import play.api.libs.json._

case class Instruction(
  override val uuid: String,
  override val modelType: String,
  override val name: String,
  override val connectionTo: String,
  override val parent: String,
  override val createdBy: String) extends DbModel 
    with JsonImpl with HasModelType with HasName with HasConnectionTo 
    with HasParent 
    with HasCreatedBy {

  override def asJsObject: JsObject = {
    Instruction.hasModelTypeJsObject(this) ++ 
    Instruction.connectionToJsObject(this) ++ 
    Instruction.namedModelJsObject(this) ++
    Instruction.childOfJsObject(this) ++ 
    Instruction.createdByJsObject(this)
  }
}

object Instruction extends DbModelComp[Instruction] with HasModelTypeComp with 
  HasParentComp[DbModel with HasAmlId] 
  with HasConnectionToComp[Domain] with HasCreatedByComp with HasNameComp {
  implicit val instructionFormat = Json.format[Instruction]

  private val KeyCreatedByUser = "createdByUser"

  def create(name: String, connectionToToDomain: String, parentAmlObject: String, createdBy: String) =
    Instruction(uuid = UUID.randomUUID.toString, modelType=Types.InstructionType.stringValue,
        name = name, connectionTo = connectionToToDomain, parent = parentAmlObject, createdBy = createdBy)
}

