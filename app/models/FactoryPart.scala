package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait FactoryPart {
  val factory: String
}

object FactoryPart {

  private val KeyFactory = "factory"

  def queryByFactory(factory: Factory): JsObject = Json.obj(KeyFactory -> JsString(factory.uuid))

  def asJsObject(model: FactoryPart): JsObject = Json.obj(KeyFactory -> JsString(model.factory))
}
