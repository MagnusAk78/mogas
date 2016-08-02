package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait NamedModel {
  val name: String
}

trait NamedModelComp {
  private val KeyName = "name"

  def queryByName(name: String): JsObject = Json.obj(KeyName -> JsString(name))

  def namedModelJsObject(namedModel: NamedModel): JsObject = Json.obj(KeyName -> JsString(namedModel.name))
}
