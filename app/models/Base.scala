package models

import play.api.libs.json.JsObject
import play.api.libs.json._

trait BaseModel extends Updatable {
  val uuid: String
  
  def uuidQuery: JsObject = JsObject(BaseModel.getKeyValueSet(this))
}

trait Updatable {
  def updateQuery: JsObject
}

object BaseModel {

  val KeyUUID = "uuid"

  def uuidInSetQuery(uuids: Set[String]): JsObject = Json.obj(KeyUUID -> Json.obj("$in" -> Json.toJson(uuids)))
  
  def getKeyValueSet(baseModel: BaseModel): Seq[JsField] = {
      Seq(KeyUUID -> JsString(baseModel.uuid))
  }
  
  def uuidQuery(uuid: String): JsObject = JsObject(Seq(KeyUUID -> JsString(uuid)))
}
