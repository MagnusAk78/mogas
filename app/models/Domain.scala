package models

import java.util.UUID
import play.api.libs.json._

case class Domain(
    override val uuid: String,
    override val name: String,
    val allowedUsers: Set[String],
    hierachies: Set[String]) extends DbModel with NamedModel {

  override def asJsObject: JsObject =
    JsObject(Seq(Domain.KeyDomainHierachies -> Json.toJson(hierachies))) ++
      Domain.namedModelJsObject(this) ++
      Json.obj(Domain.KeyAllowedUsers -> Json.toJson(allowedUsers))
}

object Domain extends DbModelComp[Domain] with ChildOfComp[Domain] with NamedModelComp {
  implicit val domainFormat = Json.format[Domain]

  private val KeyAllowedUsers = "allowedUsers"
  private val KeyDomainHierachies = "domainHierachies"

  def create(name: String, allowedUsers: Set[String] = Set.empty, hierachies: Set[String] = Set.empty) =
    Domain(uuid = UUID.randomUUID.toString, name = name, allowedUsers = allowedUsers,
      hierachies = hierachies)

  def queryByAllowedUser(allowedUser: User): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser.uuid))
}
