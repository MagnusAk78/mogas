package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

trait HasShortText {
  val shortText: String
}

trait HasShortTextComp {

  private val KeyShortText = "shortText"

  def hasShortTextJsObject(hasShortText: HasShortText): JsObject = 
    Json.obj(KeyShortText -> JsString(hasShortText.shortText))
}
