package models

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

/**
 * A DbModel is a model that is represented in the database.
 */
trait DbModel {
  val uuid: String
}

trait DbModelComp[M <: DbModel] {

  private val KeyUUID = "uuid"

  final val queryAll: JsObject = Json.obj()

  def queryByUuid(uuid: String): JsObject = Json.obj(KeyUUID -> JsString(uuid))

  def queryBySetOfUuids(uuids: Set[String]): JsObject = Json.obj(KeyUUID -> Json.obj("$in" -> Json.toJson(uuids)))

  def getUpdateObject(newDocument: JsValue): JsObject = Json.obj("$set" -> newDocument)
}
