package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

object AmlFiles {

  final val OctetStreamContentType = "application/octet-stream"

  private val KeyBelongToFactory = "belongToFactory"

  val KeyMetadata = "metadata"

  def getQueryAllAmlFiles(belongToUuid: String): JsObject =
    Json.obj((KeyMetadata + "." + KeyBelongToFactory) -> JsString(belongToUuid))

  def getAmlFileMetadata(factoryUuid: String) =
    Json.obj(
      KeyBelongToFactory -> JsString(factoryUuid))
}