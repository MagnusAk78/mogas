package models

case class Organisation(
    id: Option[String] = None,
    name: Option[String] = None,
    allowedUsers: Set[String] = Set.empty) extends BaseModel
    
object Organisation {
  import play.api.libs.json._
  
  object Keys {
    case object Id extends ModelKey("_id")
    case object Name extends ModelKey("name")
    case object AllowedUsers extends ModelKey("allowedUsers")
  }

  implicit object OrganisationWrites extends OWrites[Organisation] {
      def writes(organisation: Organisation): JsObject = {
        val sequence: Seq[JsField] = Seq[JsField]() ++
          organisation.id.map(Keys.Id.value -> JsString(_)) ++
          organisation.name.map(Keys.Name.value -> JsString(_)) ++
          List(Keys.AllowedUsers.value -> JsArray(organisation.allowedUsers.map(JsString(_)).toSeq))
          
        JsObject(sequence)
      }
  }

  implicit object OrganisationReads extends Reads[Organisation] {
    def reads(json: JsValue): JsResult[Organisation] = json match {
      case obj: JsObject => try {
        val id = (obj \ Keys.Id.value).asOpt[String]
        val name = (obj \ Keys.Name.value).asOpt[String]
        val allowedUsers = (obj \ Keys.AllowedUsers.value).as[Set[String]]

        JsSuccess(Organisation(id, name, allowedUsers))
        
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}