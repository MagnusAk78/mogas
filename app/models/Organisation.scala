package models

import java.util.UUID

case class Organisation(
    override val uuid: Option[String] = None,
    val name: Option[String] = None,
    val allowedUsers: Set[String] = Set.empty,
    val imageReadFileId: Option[String] = None) extends BaseModel {
}

object Organisation extends BaseModelCompanion {
  import play.api.libs.json._
  
  import reactivemongo.play.json.Writers._
  
  val KeyName = "name"
  val KeyAllowedUsers = "allowedUsers"
  val KeyImageReadFileId = "imageReadFileId"

  def create(name: Option[String] = None, allowedUsers: Set[String] = Set.empty, imageReadFileId: Option[String] = None) =
    Organisation(uuid = Some(UUID.randomUUID.toString), name = name, allowedUsers, imageReadFileId)

  implicit object OrganisationWrites extends OWrites[Organisation] {
    def writes(organisation: Organisation): JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        organisation.uuid.map(KeyUUID -> JsString(_)) ++
        organisation.name.map(KeyName -> JsString(_)) ++
        List(KeyAllowedUsers -> JsArray(organisation.allowedUsers.map(JsString(_)).toSeq)) ++
        organisation.imageReadFileId.map(KeyImageReadFileId -> JsString(_))

      JsObject(sequence)
    }
  }

  implicit object OrganisationReads extends Reads[Organisation] {
    def reads(json: JsValue): JsResult[Organisation] = json match {
      case obj: JsObject => try {
        val uuid = (obj \ KeyUUID).asOpt[String]
        val name = (obj \ KeyName).asOpt[String]
        val allowedUsers = (obj \ KeyAllowedUsers).as[Set[String]]
        val imageReadFileId = (obj \ KeyImageReadFileId).asOpt[String]

        JsSuccess(Organisation(uuid, name, allowedUsers, imageReadFileId))

      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}