package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait NamedModel {
  val name: String
}

object NamedModel {
  private val KeyName = "name"

  def queryByName(name: String): JsObject = Json.obj(KeyName -> JsString(name))

  def asJsObject(namedModel: NamedModel): JsObject = Json.obj(KeyName -> JsString(namedModel.name))
}
