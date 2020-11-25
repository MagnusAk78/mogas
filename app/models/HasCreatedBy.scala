package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait HasCreatedBy {
  val createdBy: String
}

object HasCreatedBy {
  private val KeyCreatedBy = "createdBy"

  def queryByCreatedBy(createdBy: User): JsObject = Json.obj(KeyCreatedBy -> JsString(createdBy.uuid))
}