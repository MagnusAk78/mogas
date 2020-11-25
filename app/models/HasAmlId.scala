package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasAmlId {
  val amlId: String
}

object HasAmlId {
  private val KeyAmlId = "amlId"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))
}
