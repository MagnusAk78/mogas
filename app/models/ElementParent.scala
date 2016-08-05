package models

import play.api.libs.json.JsObject
import play.api.libs.json.Json

trait ElementParent extends DbModel {
  val elements: Set[String]
}

trait ElementParentComp {

  private val KeyElements = "elements"

  def elementParentJsObject(elementParent: ElementParent): JsObject =
    Json.obj(KeyElements -> Json.toJson(elementParent.elements))
}