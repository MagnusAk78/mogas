package models

import java.util.UUID

import play.api.libs.json._

case class Organisation(
    override val uuid: String,
    val name: String,
    val allowedUsers: Set[String]) extends BaseModel {

    override def updateQuery: JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        Organisation.getKeyValueSet(this)
        
      Json.obj("$set" -> JsObject(sequence))
    }
}
    
object Organisation {
  
  implicit val organisationFormat = Json.format[Organisation]
    
  private val KeyName = "name"
  private val KeyAllowedUsers = "allowedUsers"

  def create(name: String, allowedUsers: Set[String] = Set.empty, imageReadFileId: String = UuidNotSet) =
    Organisation(uuid = UUID.randomUUID.toString, name = name, allowedUsers = allowedUsers)
    
  def nameQuery(name: String): JsObject = Json.obj(KeyName -> JsString(name))
  
  def allowedUserQuery(allowedUser: String): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser))
  
  def getKeyValueSet(organisation: Organisation): Seq[JsField] = {
      Seq(KeyName -> JsString(organisation.name)) ++
      Seq(KeyAllowedUsers -> Json.toJson(organisation.allowedUsers))
  }
}