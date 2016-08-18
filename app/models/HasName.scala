package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasName {
  val name: String
}

trait HasNameComp {
  private val KeyName = "name"

  def queryByName(name: String): JsObject = Json.obj(KeyName -> JsString(name))

  def namedModelJsObject(namedModel: HasName): JsObject = Json.obj(KeyName -> JsString(namedModel.name))
}
