package models

import java.util.UUID

import play.api.libs.json._

case class Organisation(
    override val uuid: String,
    val name: String,
    val allowedUsers: Set[String],
    val imageReadFileId: String) extends BaseModel {
}

case class OrganisationUpdate(
    val name: Option[String] = None,
    val allowedUsers: Option[Set[String]] = None,
    val imageReadFileId: Option[String] = None) extends BaseModelUpdate {
  
  override def toSetJsObj: JsObject = {
      val sequence: Seq[JsField] = Seq[JsField]() ++
        name.map(Organisation.KeyName -> JsString(_)) ++
        allowedUsers.map(Organisation.KeyAllowedUsers -> Json.toJson(_)) ++
        imageReadFileId.map(Organisation.KeyImageReadFileId -> JsString(_))

      Json.obj("$set" -> JsObject(sequence))
    }
}
    
object Organisation extends BaseModelCompanion {
  
  import reactivemongo.play.json.Writers._
  
  val KeyName = "name"
  val KeyAllowedUsers = "allowedUsers"
  val KeyImageReadFileId = "imageReadFileId"
  
  implicit val organisationFormat = Json.format[Organisation]

  def create(name: String, allowedUsers: Set[String] = Set.empty, imageReadFileId: String = UuidNotSet) =
    Organisation(uuid = UUID.randomUUID.toString, name = name, allowedUsers = allowedUsers, imageReadFileId = imageReadFileId)
    
  def nameQuery(name: String): JsObject = Json.obj(KeyName -> JsString(name))
  
  def allowedUserQuery(allowedUser: String): JsObject = Json.obj(KeyAllowedUsers -> JsString(allowedUser))
}