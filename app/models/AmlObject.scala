package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait AmlObject extends DbModel with ConnectionTo[Factory] {
  val amlId: String
}

trait AmlObjectComp extends ConnectionToComp[Factory] {

  private val KeyAmlId = "amlId"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))

  def amlObjectJsObject(amlObject: AmlObject): JsObject =
    Json.obj(KeyAmlId -> JsString(amlObject.amlId)) ++ connectionToJsObject(amlObject)
}
