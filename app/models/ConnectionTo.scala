package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait ConnectionTo[M <: DbModel] {
  val connectionTo: String
}

trait ConnectionToComp[M <: DbModel] {

  private val KeyConnectionTo = "connectionTo"

  def queryByConnectionTo(model: M): JsObject = Json.obj(KeyConnectionTo -> JsString(model.uuid))

  def connectionToJsObject(connectionTo: ConnectionTo[M]): JsObject =
    Json.obj(KeyConnectionTo -> JsString(connectionTo.connectionTo))
}
