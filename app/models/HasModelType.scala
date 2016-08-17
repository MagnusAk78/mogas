package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasModelType {
  val modelType: String
}

trait HasModelTypeComp {

  private val KeyModelType = "modelType"

  def hasModelTypeJsObject(hasModelType: HasModelType): JsObject = Json.obj(KeyModelType -> JsString(hasModelType.modelType))
}
