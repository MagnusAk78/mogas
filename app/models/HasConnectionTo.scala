package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait HasConnectionTo {
  val connectionTo: String
}

trait HasConnectionToComp[M <: DbModel] {

  private val KeyConnectionTo = "connectionTo"

  def queryByHasConnectionTo(model: M): JsObject = Json.obj(KeyConnectionTo -> JsString(model.uuid))

  def connectionToJsObject(connectionTo: HasConnectionTo): JsObject =
    Json.obj(KeyConnectionTo -> JsString(connectionTo.connectionTo))
}
