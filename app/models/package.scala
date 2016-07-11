import play.api.libs.json.JsValue
import play.api.libs.json.JsObject


package object models {
  import play.api.libs.json._
  
  type JsField = (String, JsValue)
}