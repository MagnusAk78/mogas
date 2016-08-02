package models

import play.api.libs.json.JsObject
import play.api.libs.json.Json

trait InternalElementParent extends DbModel {
  val internalElements: Set[String]
}

trait InternalElementParentComp {

  private val KeyInternalElements = "internalElements"

  def internalElementParentJsObject(internalElementParent: InternalElementParent): JsObject =
    Json.obj(KeyInternalElements -> Json.toJson(internalElementParent.internalElements))
}