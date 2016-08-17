package models

import java.util.UUID
import play.api.libs.json._

case class Interface(
    override val uuid: String,
    override val modelType: String,
    override val connectionTo: String,
    override val amlId: String,
    override val parent: String,
    override val orderNumber: Int,
    override val name: String) extends DbModel with JsonImpl with HasModelType with AmlObject with ChildOf[Element] {

  override def asJsObject: JsObject =
    Interface.hasModelTypeJsObject(this) ++
    Interface.amlObjectJsObject(this) ++
      Interface.childOfJsObject(this)
}

object Interface extends DbModelComp[Interface] with HasModelTypeComp with AmlObjectComp 
  with ChildOfComp[Element] {
  implicit val interfaceFormat = Json.format[Interface]

  def create(connectionToDomain: String, name: String, parent: String, orderNumber: Int, amlId: String) =
    Interface(uuid = UUID.randomUUID.toString, modelType=Types.InterfaceType.stringValue, connectionTo = connectionToDomain, 
        name = name, parent = parent, orderNumber = orderNumber, amlId = amlId)
}
