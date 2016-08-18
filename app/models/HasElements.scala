package models

import play.api.libs.json.JsObject
import play.api.libs.json.Json

trait HasElements {
  val elements: Set[String]
}

trait HasElementsComp {

  private val KeyElements = "elements"

  def hasElementsJsObject(hasElements: HasElements): JsObject =
    Json.obj(KeyElements -> Json.toJson(hasElements.elements))
}