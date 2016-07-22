package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait AmlParent extends DbModel

trait HierarchyPart {
  val amlId: String
  val parent: String
}

object HierarchyPart {

  private val KeyAmlId = "amlId"
  private val KeyParent = "parent"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))

  def queryByParent(parent: AmlParent): JsObject = Json.obj(KeyParent -> JsString(parent.uuid))

  def asJsObject(hierarchyPart: HierarchyPart): JsObject =
    Json.obj(KeyAmlId -> JsString(hierarchyPart.amlId),
      KeyParent -> JsString(hierarchyPart.parent))
}
