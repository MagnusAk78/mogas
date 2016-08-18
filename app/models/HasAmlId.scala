package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait HasAmlId {
  val amlId: String
}

trait HasAmlIdComp {

  private val KeyAmlId = "amlId"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))

  def amlObjectJsObject(amlObject: HasAmlId): JsObject = Json.obj(KeyAmlId -> JsString(amlObject.amlId))
}
