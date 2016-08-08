package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Videos {

  private val KeyVideoBelongTo = "videoBelongTo"

  private val KeyMetadata = "metadata"

  def videoBelongToQuery(uuid: String): JsObject = Json.obj(KeyVideoBelongTo -> JsString(uuid))

  def getVideoMetadata(belongToUuid: String) =
    Json.obj(KeyVideoBelongTo -> JsString(belongToUuid))

  def getQueryAllVideos(belongToUuid: String): JsObject =
    Json.obj((KeyMetadata + "." + KeyVideoBelongTo) -> JsString(belongToUuid))

  def getBelongsToFromMetadata(metadata: JsObject): Option[String] = {
    (metadata \ KeyVideoBelongTo).asOpt[String]
  }
}
