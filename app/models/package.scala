package object models {
  import play.api.libs.json.JsValue

  type JsField = (String, JsValue)
  
  val UuidNotSet = "NOT_SET"
}