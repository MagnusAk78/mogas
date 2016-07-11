package models

import java.util.UUID

case class Organisation(
    override val uuid: Option[String] = None,
    val name: Option[String] = None,
    val allowedUsers: Set[String] = Set.empty) extends BaseModel {
}

object Organisation extends BaseModelCompanion {
  import play.api.libs.json._

  val KeyName = "name"
  val KeyAllowedUsers = "allowedUsers"

  def create(name: Option[String] = None, allowedUsers: Set[String] = Set.empty) =
    Organisation(uuid = Some(UUID.randomUUID.toString), name, allowedUsers)

  implicit object OrganisationWrites extends OWrites[Organisation] {
    def writes(organisation: Organisation): JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        organisation.uuid.map(KeyUUID -> JsString(_)) ++
        organisation.name.map(KeyName -> JsString(_)) ++
        List(KeyAllowedUsers -> JsArray(organisation.allowedUsers.map(JsString(_)).toSeq))

      JsObject(sequence)
    }
  }

  implicit object OrganisationReads extends Reads[Organisation] {
    def reads(json: JsValue): JsResult[Organisation] = json match {
      case obj: JsObject => try {
        val uuid = (obj \ KeyUUID).asOpt[String]
        val name = (obj \ KeyName).asOpt[String]
        val allowedUsers = (obj \ KeyAllowedUsers).as[Set[String]]

        JsSuccess(Organisation(uuid, name, allowedUsers))

      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}