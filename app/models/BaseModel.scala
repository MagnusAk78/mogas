package models

import play.api.libs.json.JsObject


trait BaseModel {
  val uuid: String
}

trait BaseModelUpdate {
  def toSetJsObj: JsObject
}

trait BaseModelCompanion {
  
  import play.api.libs.json._
  
  val KeyUUID = "uuid"
  
  def uuidQuery(uuid: String): JsObject = Json.obj(KeyUUID -> JsString(uuid))
  
  def uuidInSetQuery(uuids: Set[String]): JsObject = Json.obj(KeyUUID -> Json.obj("$in" -> Json.toJson(uuids)))
}
