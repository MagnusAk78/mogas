package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasText {
  val text: String
}

trait HasTextComp {

  private val KeyText = "text"

  def hasTextJsObject(hasText: HasText): JsObject = Json.obj(KeyText -> JsString(hasText.text))
}
