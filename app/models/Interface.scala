package models

import java.util.UUID
import play.api.libs.json._

case class Interface(
    override val uuid: String,
    override val connectionTo: String,
    override val amlId: String,
    override val parent: String,
    override val orderNumber: Int,
    override val name: String) extends DbModel with AmlObject with ChildOf[Element] {

  override def asJsObject: JsObject =
    Interface.amlObjectJsObject(this) ++
      Interface.childOfJsObject(this)
}

object Interface extends DbModelComp[Interface] with AmlObjectComp with ChildOfComp[Element] {
  implicit val interfaceFormat = Json.format[Interface]

  def create(connectionToDomain: String, name: String, parent: String, orderNumber: Int, amlId: String) =
    Interface(uuid = UUID.randomUUID.toString, connectionTo = connectionToDomain, name = name,
      parent = parent, orderNumber = orderNumber, amlId = amlId)
}
