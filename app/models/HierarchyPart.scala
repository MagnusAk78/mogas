package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait HierarchyPart {
  val amlId: String
  val parent: String
}

object HierarchyPart {

  private val KeyAmlId = "amlId"
  private val KeyParent = "parent"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))

  def queryByParent(parent: String): JsObject = Json.obj(KeyParent -> JsString(parent))

  def asJsObject(hierarchyPart: HierarchyPart): JsObject =
    Json.obj(KeyAmlId -> JsString(hierarchyPart.amlId),
      KeyParent -> JsString(hierarchyPart.parent))
}
