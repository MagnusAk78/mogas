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
    val activeOrganisation: String) extends BaseModel with Identity {

  override def updateQuery: JsObject = {
    val sequence: Seq[JsField] = Seq[JsField]() ++
      User.getKeyValueSet(this)

    Json.obj("$set" -> JsObject(sequence))
  }
}

object User {

  implicit val userFormat = Json.format[User]

  private val KeyLoginInfo = "loginInfo"
  private val KeyFirstName = "firstName"
  private val KeyLastName = "lastName"
  private val KeyFullName = "fullName"
  private val KeyEmail = "email"
  private val KeyAvatarURL = "avatarURL"
  private val KeyActiveOrganisation = "activeOrganisation"

  def create(loginInfo: LoginInfo,
             firstName: String,
             lastName: String,
             fullName: String,
             email: String,
             avatarURL: Option[String] = None,
             activeOrganisation: String = UuidNotSet) =
    User(uuid = UUID.randomUUID.toString, loginInfo = loginInfo, firstName = firstName,
      lastName = lastName, fullName = fullName, email = email, avatarURL = avatarURL,
      activeOrganisation = activeOrganisation)

  def loginInfoQuery(loginInfo: LoginInfo): JsObject = Json.obj(KeyLoginInfo -> Json.toJson(loginInfo))

  def activeOrganisationQuery(activeOrganisationUuid: String): JsObject = Json.obj(KeyActiveOrganisation -> JsString(activeOrganisationUuid))

  def getKeyValueSet(user: User): Seq[JsField] = {
    Seq[JsField]() ++
      Seq(KeyLoginInfo -> Json.toJson(user.loginInfo)) ++
      Seq(KeyFirstName -> JsString(user.firstName)) ++
      Seq(KeyLastName -> JsString(user.lastName)) ++
      Seq(KeyFullName -> JsString(user.fullName)) ++
      Seq(KeyEmail -> JsString(user.email)) ++
      user.avatarURL.map(KeyAvatarURL -> JsString(_)) ++
      Seq(User.KeyActiveOrganisation -> JsString(user.activeOrganisation))
  }
}