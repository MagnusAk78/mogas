package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Images {
  
  sealed trait ImageSize {
    val stringValue: String
    val pixels: Int
  }
  
  final case object Standard extends ImageSize {
    override val stringValue = "Standard"
    override val pixels = 800
  }
  
  final case object Thumbnail extends ImageSize {
    override val stringValue = "Thumbnail"
    override val pixels = 320
  }
 
  private val KeyImageBelongTo = "imageBelongTo"
  private val KeyImageSize = "imageSize"
  
  private val KeyMetadata = "metadata"
  
  def imageBelongToQuery(uuid: String): JsObject = Json.obj(KeyImageBelongTo -> JsString(uuid))
  
  def getImageMetadata(belongToUuid: String, imageSize: ImageSize) = 
    Json.obj(
        KeyImageBelongTo -> JsString(belongToUuid),
        KeyImageSize -> JsString(imageSize.stringValue))
  
  def getQueryImage(belongToUuid: String, imageSize: ImageSize): JsObject =
    Json.obj(KeyMetadata -> getImageMetadata(belongToUuid, imageSize))
        
  def getQueryAllImages(belongToUuid: String): JsObject = 
    Json.obj((KeyMetadata + "." + KeyImageBelongTo) -> JsString(belongToUuid))        
  
  def getBelongsToFromMetadata(metadata: JsObject): Option[String] = {
    (metadata \ KeyImageBelongTo).asOpt[String]
  }
}
