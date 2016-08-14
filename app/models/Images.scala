package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Images {

  sealed trait ImageSize {
    val stringValue: String
    val xPixels: Int
    val yPixels: Int
  }

  final case object Standard extends ImageSize {
    override val stringValue = "Standard"
    override val xPixels = 1024
    override val yPixels = 768
  }

  final case object Thumbnail extends ImageSize {
    override val stringValue = "Thumbnail"
    override val xPixels = 240
    override val yPixels = 200
  }

  private val KeyImageBelongTo = "imageBelongTo"
  private val KeyImageSize = "imageSize"

  private val KeyMetadata = "metadata"

  private def imageBelongToQuery(uuid: String): JsObject = Json.obj(KeyImageBelongTo -> JsString(uuid))

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
