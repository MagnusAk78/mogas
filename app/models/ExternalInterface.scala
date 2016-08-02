package models

import java.util.UUID
import play.api.libs.json._

case class ExternalInterface(
  override val uuid: String,
  override val connectionTo: String,
  override val amlId: String,
  override val parent: String,
  override val orderNumber: Int,
  override val name: String) extends DbModel with AmlObject with NamedModel
    with OrderedModel with ChildOf[InternalElement] {

  override def asJsObject: JsObject =
    ExternalInterface.amlObjectJsObject(this) ++
      ExternalInterface.namedModelJsObject(this) ++
      ExternalInterface.orderedModelJsObject(this) ++
      ExternalInterface.childOfJsObject(this)
}

object ExternalInterface extends DbModelComp[ExternalInterface] with ChildOfComp[InternalElement] with AmlObjectComp
    with NamedModelComp with OrderedModelComp {
  implicit val externalInterfaceFormat = Json.format[ExternalInterface]

  def create(connectionToFactory: String, name: String, parent: String, orderNumber: Int, amlId: String) =
    ExternalInterface(uuid = UUID.randomUUID.toString, connectionTo = connectionToFactory, name = name,
      parent = parent, orderNumber = orderNumber, amlId = amlId)
}
