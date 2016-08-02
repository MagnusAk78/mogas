package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import play.api.Logger
import java.util.UUID
import play.api.libs.json._

case class User(
    override val uuid: String,
    override val name: String,
    val loginInfo: LoginInfo,
    val firstName: String,
    val lastName: String,
    val email: String,
    val avatarURL: Option[String],
    val activeOrganisation: String) extends DbModel with Identity with NamedModel {

  override def asJsObject: JsObject = User.namedModelJsObject(this) ++
    JsObject(Seq.empty ++
      Seq(User.KeyLoginInfo -> Json.toJson(loginInfo)) ++
      Seq(User.KeyFirstName -> JsString(firstName)) ++
      Seq(User.KeyLastName -> JsString(lastName)) ++
      Seq(User.KeyEmail -> JsString(email)) ++
      avatarURL.map(User.KeyAvatarURL -> JsString(_)) ++
      Seq(User.KeyActiveOrganisation -> JsString(activeOrganisation)))
}

object User extends DbModelComp[User] with NamedModelComp {
  implicit val userFormat = Json.format[User]

  private val KeyLoginInfo = "loginInfo"
  private val KeyFirstName = "firstName"
  private val KeyLastName = "lastName"
  private val KeyEmail = "email"
  private val KeyAvatarURL = "avatarURL"
  private val KeyActiveOrganisation = "activeOrganisation"

  def create(loginInfo: LoginInfo,
             firstName: String,
             lastName: String,
             name: String,
             email: String,
             avatarURL: Option[String] = None,
             activeOrganisation: String = UuidNotSet) =
    User(uuid = UUID.randomUUID.toString, loginInfo = loginInfo, firstName = firstName,
      lastName = lastName, name = name, email = email, avatarURL = avatarURL,
      activeOrganisation = activeOrganisation)

  def queryByLoginInfo(loginInfo: LoginInfo): JsObject = Json.obj(KeyLoginInfo -> Json.toJson(loginInfo))

  def queryByActiveOrganisation(activeOrganisationUuid: String): JsObject =
    Json.obj(KeyActiveOrganisation -> JsString(activeOrganisationUuid))
}