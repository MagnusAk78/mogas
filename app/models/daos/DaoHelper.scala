package models.daos

import play.api.libs.json.JsObject
import play.api.libs.json.Json

object DaoHelper {
  def getSortByJsObject(key: String, ascending: Boolean): JsObject = Json.obj(key -> (if (ascending) 1 else -1))
}