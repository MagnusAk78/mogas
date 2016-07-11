package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.Logger
import java.util.UUID

case class User(
  override val uuid: Option[String] = None,
  val loginInfo: Option[LoginInfo] = None,
  val firstName: Option[String] = None,
  val lastName: Option[String] = None,
  val fullName: Option[String] = None,
  val email: Option[String] = None,
  val avatarURL: Option[String] = None,
  val activeOrganisation: Option[String] = None) extends BaseModel with Identity

object User extends BaseModelCompanion {
  import play.api.libs.json._

  val KeyLoginInfo = "loginInfo"
  val KeyFirstName = "firstName"
  val KeyLastName = "lastName"
  val KeyFullName = "fullName"
  val KeyEmail = "email"
  val KeyAvatarURL = "avatarURL"
  val KeyActiveOrganisation = "activeOrganisation"

  def create(loginInfo: Option[LoginInfo] = None,
             firstName: Option[String] = None,
             lastName: Option[String] = None,
             fullName: Option[String] = None,
             email: Option[String] = None,
             avatarURL: Option[String] = None,
             activeOrganisation: Option[String] = None) =
    User(uuid = Some(UUID.randomUUID.toString), loginInfo = loginInfo, firstName = firstName,
      lastName = lastName, fullName = fullName, email = email, avatarURL = avatarURL,
      activeOrganisation = activeOrganisation)

  implicit object UserWrites extends OWrites[User] {
    def writes(user: User): JsObject = JsObject(
      Seq[JsField]() ++
        user.uuid.map(KeyUUID -> JsString(_)) ++
        user.loginInfo.map(KeyLoginInfo -> Json.toJson(_)) ++
        user.firstName.map(KeyFirstName -> JsString(_)) ++
        user.lastName.map(KeyLastName -> JsString(_)) ++
        user.fullName.map(KeyFullName -> JsString(_)) ++
        user.email.map(KeyEmail -> JsString(_)) ++
        user.avatarURL.map(KeyAvatarURL -> JsString(_)) ++
        user.activeOrganisation.map(KeyActiveOrganisation -> JsString(_)))
  }

  implicit object UserReads extends Reads[User] {
    Logger.info("UserReads")
    def reads(json: JsValue): JsResult[User] = {
      json match {
        case obj: JsObject => try {
          val uuid = (obj \ KeyUUID).asOpt[String]
          val loginInfo = (obj \ KeyLoginInfo).asOpt[LoginInfo]
          val firstName = (obj \ KeyFirstName).asOpt[String]
          val lastName = (obj \ KeyLastName).asOpt[String]
          val fullName = (obj \ KeyFullName).asOpt[String]
          val email = (obj \ KeyEmail).asOpt[String]
          val avatarURL = (obj \ KeyAvatarURL).asOpt[String]
          val activeOrganisation = (obj \ KeyActiveOrganisation).asOpt[String]

          JsSuccess(User(uuid, loginInfo, firstName, lastName, fullName, email, avatarURL))

        } catch {
          case cause: Throwable => JsError(cause.getMessage)
        }

        case _ => JsError("expected.jsobject")
      }
    }
  }
}