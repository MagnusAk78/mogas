package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait AmlObject extends DbModel with NamedModel with ConnectionTo[Domain] with OrderedModel {
  val amlId: String
}

object AmlObject extends DbModelComp[AmlObject]

trait AmlObjectComp extends NamedModelComp with ConnectionToComp[Domain] with OrderedModelComp {

  private val KeyAmlId = "amlId"

  def queryByAmlId(amlId: String): JsObject = Json.obj(KeyAmlId -> JsString(amlId))

  def amlObjectJsObject(amlObject: AmlObject): JsObject =
    Json.obj(KeyAmlId -> JsString(amlObject.amlId)) ++
      namedModelJsObject(amlObject) ++
      connectionToJsObject(amlObject) ++
      orderedModelJsObject(amlObject)
}
