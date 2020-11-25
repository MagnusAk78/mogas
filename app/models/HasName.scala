package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasName {
  val name: String
}

object HasName {
  private val KeyName = "name"

  def queryByName(name: String): JsObject = Json.obj(KeyName -> JsString(name))
}
