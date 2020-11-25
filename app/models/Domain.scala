package models

import java.util.UUID

import play.api.libs.json.{Json, JsObject, JsString}

case class Domain(
    override val uuid: String,
    override val modelType: String = Types.DomainType.stringValue,
    override val name: String,
    val allowedUsers: Set[String],
    val domainHierachies: Set[String]) extends DbModel with HasModelType with HasName {
}

object Domain extends DbModelComp[Domain] with HasModelTypeComp with HasParentComp[Domain] with HasNameComp {

  implicit val domainFormat = Json.format[Domain]

  private val KeyAllowedUsers = "allowedUsers"
  private val KeyDomainHierachies = "domainHierachies"

  def create(name: String, allowedUsers: Set[String] = Set.empty, domainHierachies: Set[String] = Set.empty) =
    Domain(uuid = UUID.randomUUID.toString, modelType=Types.DomainType.stringValue, name = name, 
        allowedUsers = allowedUsers, domainHierachies = domainHierachies)

  def queryByAllowedUser(allowedUser: User): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser.uuid))
}
