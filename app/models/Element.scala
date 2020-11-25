package models

import java.util.UUID
import play.api.libs.json.Json

case class Element(
  override val uuid: String,
  override val modelType: String,
  override val connectionTo: String,
  override val amlId: String,
  override val parent: String,
  override val orderNumber: Int,
  override val name: String,
  override val elements: Set[String],
  parentIsHierarchy: Boolean,
  interfaces: Set[String]) extends DbModel with HasModelType with HasConnectionTo with HasAmlId
    with HasParent with OrderedModel with HasName with HasElements {
}

object Element {

  implicit val elementFormat = Json.format[Element]

  private val KeyParentIsHierarchy = "parentIsHierarchy"
  private val KeyInterfaces = "interfaces"

  def create(connectionToDomain: String, name: String, parent: String, parentIsHierarchy: Boolean = false,
             orderNumber: Int, amlId: String, elements: Set[String] = Set.empty,
             interfaces: Set[String] = Set.empty) =
    Element(uuid = UUID.randomUUID.toString, modelType = Types.ElementType.stringValue, connectionTo = connectionToDomain, 
        name = name, parent = parent, parentIsHierarchy = parentIsHierarchy, orderNumber = orderNumber, amlId = amlId,
      elements = elements, interfaces = interfaces)
}
