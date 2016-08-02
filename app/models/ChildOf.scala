package models

import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsObject

trait ChildOf[M <: DbModel] {
  val parent: String
}

trait ChildOfComp[M <: DbModel] {

  private val KeyParent = "parent"

  def queryByParent(parentModel: M): JsObject = Json.obj(KeyParent -> JsString(parentModel.uuid))

  def childOfJsObject(child: ChildOf[M]): JsObject =
    Json.obj(KeyParent -> JsString(child.parent))
}
