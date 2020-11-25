package models

import java.util.UUID
import play.api.libs.json.Json

case class Hierarchy(
  override val uuid: String,
  override val modelType: String,
  override val parent: String,
  override val name: String,
  override val orderNumber: Int,
  override val elements: Set[String]) extends DbModel with HasModelType with
    HasParent with HasElements with HasName with OrderedModel {
}

object Hierarchy extends DbModelComp[Hierarchy] with HasModelTypeComp with HasParentComp[Domain] with HasElementsComp
    with HasNameComp with OrderedModelComp {

  implicit val hierarchyFormat = Json.format[Hierarchy]

  def create(name: String, parentDomain: String, orderNumber: Int, elements: Set[String] = Set.empty) =
    Hierarchy(uuid = UUID.randomUUID.toString, modelType=Types.HierarchyType.stringValue, name = name, 
        parent = parentDomain, orderNumber = orderNumber, elements = elements)
}
