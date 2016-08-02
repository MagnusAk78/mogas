package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait CreatedBy {
  val createdBy: String
}

trait CreatedByComp {

  private val KeyCreatedBy = "createdBy"

  def queryByCreatedBy(createdBy: User): JsObject = Json.obj(KeyCreatedBy -> JsString(createdBy.uuid))

  def createdByJsObject(model: CreatedBy): JsObject =
    Json.obj(KeyCreatedBy -> JsString(model.createdBy))
}
