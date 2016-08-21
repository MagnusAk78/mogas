package models

import java.util.UUID
import play.api.libs.json._

case class Domain(
    override val uuid: String,
    override val modelType: String = Types.DomainType.stringValue,
    override val name: String,
    val allowedUsers: Set[String],
    hierachies: Set[String]) extends DbModel with JsonImpl with HasModelType with HasName {

  override def asJsObject: JsObject =
    JsObject(
      Seq(Domain.KeyDomainHierachies -> Json.toJson(hierachies))) ++
      Domain.namedModelJsObject(this) ++
      Domain.hasModelTypeJsObject(this) ++
      Json.obj(Domain.KeyAllowedUsers -> Json.toJson(allowedUsers))
}

object Domain extends DbModelComp[Domain] with HasModelTypeComp with HasParentComp[Domain] with HasNameComp {
  implicit val domainFormat = Json.format[Domain]

  private val KeyAllowedUsers = "allowedUsers"
  private val KeyDomainHierachies = "domainHierachies"

  def create(name: String, allowedUsers: Set[String] = Set.empty, hierachies: Set[String] = Set.empty) =
    Domain(uuid = UUID.randomUUID.toString, modelType=Types.DomainType.stringValue, name = name, 
        allowedUsers = allowedUsers, hierachies = hierachies)

  def queryByAllowedUser(allowedUser: User): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser.uuid))
}
