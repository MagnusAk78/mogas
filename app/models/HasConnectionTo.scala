package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait HasConnectionTo {
  val connectionTo: String
}

object HasConnectionTo {
  private val KeyConnectionTo = "connectionTo"

  def queryByHasConnectionTo(uuid: String): JsObject = Json.obj(KeyConnectionTo -> JsString(uuid))
}
