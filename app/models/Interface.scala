package models

import java.util.UUID
import play.api.libs.json.Json

case class Interface(
    override val uuid: String,
    override val modelType: String,
    override val connectionTo: String,
    override val amlId: String,
    override val parent: String,
    override val orderNumber: Int,
    override val name: String) extends DbModel with HasName
      with HasConnectionTo with OrderedModel with HasModelType with HasAmlId 
      with HasParent {
}

object Interface {

  implicit val interfaceFormat = Json.format[Interface]

  def create(connectionToDomain: String, name: String, parent: String, orderNumber: Int, amlId: String) =
    Interface(uuid = UUID.randomUUID.toString, modelType=Types.InterfaceType.stringValue, connectionTo = connectionToDomain, 
        name = name, parent = parent, orderNumber = orderNumber, amlId = amlId)
}
