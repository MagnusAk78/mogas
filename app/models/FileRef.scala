package models

import java.util.UUID

case class FileRef(
    override val uuid: Option[String] = None,
    val contentType: Option[String] = None,
    val connectedTo: Option[String] = None) extends BaseModel {
}

object FileRef extends BaseModelCompanion {
  import play.api.libs.json._

  val KeyContentType = "contentType"
  val KeyConnectedTo = "connectedTo"

  def create(contentType: Option[String] = None, connectedTo: Option[String] = None) =
    FileRef(uuid = Some(UUID.randomUUID.toString), contentType, connectedTo)

  implicit object OrganisationWrites extends OWrites[FileRef] {
    def writes(fileRef: FileRef): JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        fileRef.uuid.map(KeyUUID -> JsString(_)) ++
        fileRef.contentType.map(KeyContentType -> JsString(_)) ++
        fileRef.connectedTo.map(KeyConnectedTo -> JsString(_))

      JsObject(sequence)
    }
  }

  implicit object OrganisationReads extends Reads[FileRef] {
    def reads(json: JsValue): JsResult[FileRef] = json match {
      case obj: JsObject => try {
        val uuid = (obj \ KeyUUID).asOpt[String]
        val name = (obj \ KeyContentType).asOpt[String]
        val allowedUsers = (obj \ KeyConnectedTo).asOpt[String]

        JsSuccess(FileRef(uuid, name, allowedUsers))

      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}