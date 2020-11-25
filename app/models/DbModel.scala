package models

import play.api.libs.json.{JsObject, JsString, Json}

/**
 * A DbModel is a model that is represented in the database.
 */
trait DbModel {
  val uuid: String
}

trait JsonImpl {
  def asJsObject: JsObject
}

trait DbModelComp[M <: DbModel with JsonImpl] {

  private val KeyUUID = "uuid"

  final val queryAll: JsObject = Json.obj()

  def queryByUuid(uuid: String): JsObject = Json.obj(KeyUUID -> JsString(uuid))

  def queryBySetOfUuids(uuids: Set[String]): JsObject = Json.obj(KeyUUID -> Json.obj("$in" -> Json.toJson(uuids)))

  def getUpdateObject(model: M): JsObject = Json.obj("$set" -> model.asJsObject)
}
