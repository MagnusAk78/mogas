package models

import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json

object AmlFiles {

  final val OctetStreamContentType = "application/octet-stream"

  private val KeyBelongToDomain = "belongToDomain"

  val KeyMetadata = "metadata"

  def queryAllAmlFiles(belongToUuid: String): JsObject =
    Json.obj((KeyMetadata + "." + KeyBelongToDomain) -> JsString(belongToUuid))

  def amlFileMetadata(domainUuid: String) = Json.obj(KeyBelongToDomain -> JsString(domainUuid))
}