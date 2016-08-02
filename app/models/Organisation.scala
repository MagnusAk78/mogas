package models

import java.util.UUID

import play.api.libs.json._

case class Organisation(
    override val uuid: String,
    override val name: String,
    val allowedUsers: Set[String]) extends DbModel with NamedModel {

  override def asJsObject: JsObject = {
    Organisation.namedModelJsObject(this) ++
      Json.obj(Organisation.KeyAllowedUsers -> Json.toJson(allowedUsers))
  }
}

object Organisation extends DbModelComp[Organisation] with NamedModelComp {

  implicit val organisationFormat = Json.format[Organisation]

  private val KeyAllowedUsers = "allowedUsers"

  def create(name: String, allowedUsers: Set[String] = Set.empty, imageReadFileId: String = UuidNotSet) =
    Organisation(uuid = UUID.randomUUID.toString, name = name, allowedUsers = allowedUsers)

  def queryByAllowedUser(allowedUser: String): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser))
}