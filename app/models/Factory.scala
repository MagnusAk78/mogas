package models

import java.util.UUID
import play.api.libs.json._

case class Factory(
    override val uuid: String,
    override val name: String,
    override val parent: String,
    hierachies: Set[String]) extends DbModel with NamedModel with ChildOf[Organisation] {

  override def asJsObject: JsObject =
    JsObject(Seq(Factory.KeyFactoryHierachies -> Json.toJson(hierachies))) ++
      Factory.namedModelJsObject(this) ++
      Factory.childOfJsObject(this)
}

object Factory extends DbModelComp[Factory] with ChildOfComp[Organisation] with NamedModelComp {
  implicit val factoryFormat = Json.format[Factory]

  private val KeyFactoryHierachies = "factoryHierachies"

  def create(name: String, parentOrganisation: String, hierachies: Set[String] = Set.empty) =
    Factory(uuid = UUID.randomUUID.toString, name = name, parent = parentOrganisation,
      hierachies = hierachies)
}
