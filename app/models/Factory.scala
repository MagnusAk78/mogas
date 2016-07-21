package models

import java.util.UUID
import play.api.libs.json._

case class Factory(
    override val uuid: String,
    override val name: String,
    organisation: String,
    factoryHierachies: Set[String]) extends DbModel with NamedModel {

  override def asJsObject: JsObject =
    JsObject(Seq(Factory.KeyOrganisation -> Json.toJson(organisation)) ++
      Seq(Factory.KeyFactoryHierachies -> Json.toJson(factoryHierachies))) ++ NamedModel.asJsObject(this)
}

object Factory extends {
  implicit val factoryFormat = Json.format[Factory]

  private val KeyOrganisation = "organisation"
  private val KeyFactoryHierachies = "factoryHierachies"

  def create(name: String, organisation: String, factoryHierachies: Set[String] = Set.empty) =
    Factory(uuid = UUID.randomUUID.toString, name = name, organisation = organisation, factoryHierachies = factoryHierachies)

  def queryByOrganisation(organisation: String): JsObject = Json.obj(KeyOrganisation -> JsString(organisation))
}
