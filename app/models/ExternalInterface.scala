package models

import java.util.UUID
import play.api.libs.json._

case class ExternalInterface(
    override val uuid: String,
    override val factory: String,
    override val amlId: String,
    override val parent: String,
    override val orderNumber: Int,
    override val name: String) extends DbModel with FactoryPart with HierarchyPart with NamedModel with OrderedModel {

  override def asJsObject: JsObject =
    FactoryPart.asJsObject(this) ++
      OrderedModel.asJsObject(this) ++
      HierarchyPart.asJsObject(this) ++
      NamedModel.asJsObject(this)
}

object ExternalInterface {
  implicit val externalInterfaceFormat = Json.format[ExternalInterface]

  def create(factory: String, name: String, parent: String, orderNumber: Int, amlId: String) =
    ExternalInterface(uuid = UUID.randomUUID.toString, factory = factory, name = name,
      parent = parent, orderNumber = orderNumber, amlId = amlId)
}
