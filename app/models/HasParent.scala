package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait HasParent {
  val parent: String
}

object HasParent {
  private val KeyParent = "parent"

  def queryByParent(uuid: String): JsObject = Json.obj(KeyParent -> JsString(uuid))
}
