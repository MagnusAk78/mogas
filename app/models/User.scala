package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.Logger
import java.util.UUID
import play.api.libs.json._

case class User(
  override val uuid: String,
  val loginInfo: LoginInfo,
  val firstName: String,
  val lastName: String,
  val fullName: String,
  val email: String,
  val avatarURL: Option[String],
  val activeOrganisation: String,
  val imageReadFileId: String) extends BaseModel with Identity
  
case class UserUpdate(
  val loginInfo: Option[LoginInfo] = None,
  val firstName: Option[String] = None,
  val lastName: Option[String] = None,
  val fullName: Option[String] = None,
  val email: Option[String] = None,
  val avatarURL: Option[String] = None,
  val activeOrganisation: Option[String] = None,
  val imageReadFileId: Option[String] = None) extends BaseModelUpdate {
  
  override def toSetJsObj: JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        loginInfo.map(User.KeyLoginInfo -> Json.toJson(_)) ++
        firstName.map(User.KeyFirstName -> JsString(_)) ++
        lastName.map(User.KeyLastName -> JsString(_)) ++
        fullName.map(User.KeyFullName -> JsString(_)) ++
        email.map(User.KeyEmail -> JsString(_)) ++
        avatarURL.map(User.KeyAvatarURL -> JsString(_)) ++
        activeOrganisation.map(User.KeyActiveOrganisation -> JsString(_))++
        imageReadFileId.map(Organisation.KeyImageReadFileId -> JsString(_))

      Json.obj("$set" -> JsObject(sequence))
  } 
}

object User extends BaseModelCompanion {
  val KeyLoginInfo = "loginInfo"
  val KeyFirstName = "firstName"
  val KeyLastName = "lastName"
  val KeyFullName = "fullName"
  val KeyEmail = "email"
  val KeyAvatarURL = "avatarURL"
  val KeyActiveOrganisation = "activeOrganisation"
  val KeyImageReadFileId = "imageReadFileId"
  
  implicit val userFormat = Json.format[User]
  
  def loginInfoQuery(loginInfo: LoginInfo): JsObject = Json.obj(KeyLoginInfo -> Json.toJson(loginInfo))
  
  def activeOrganisationQuery(activeOrganisationUuid: String): JsObject = Json.obj(KeyActiveOrganisation -> JsString(activeOrganisationUuid))

  def create(loginInfo: LoginInfo,
             firstName: String,
             lastName: String,
             fullName: String,
             email: String,
             avatarURL: Option[String] = None,
             activeOrganisation: String = UuidNotSet,
             imageReadFileId: String = UuidNotSet) =
    User(uuid = UUID.randomUUID.toString, loginInfo = loginInfo, firstName = firstName,
      lastName = lastName, fullName = fullName, email = email, avatarURL = avatarURL,
      activeOrganisation = activeOrganisation, imageReadFileId = imageReadFileId)
}