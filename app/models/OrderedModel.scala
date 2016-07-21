package models

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsNumber

trait OrderedModel {
  val orderNumber: Int
}

object OrderedModel {
  private val KeyOrderNumber = "orderNumber"

  def queryByOrderNumber(orderNumber: Int): JsObject = Json.obj(KeyOrderNumber -> JsNumber(orderNumber))

  def asJsObject(ordered: OrderedModel): JsObject = Json.obj(KeyOrderNumber -> JsNumber(ordered.orderNumber))
}
