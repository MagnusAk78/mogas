package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

case class User(
    id: Option[String] = None,
    loginInfo: Option[LoginInfo] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    email: Option[String] = None,
    avatarURL: Option[String] = None,
    activeOrganisation: Option[String] = None) extends BaseModel with Identity
    
object User {
  import play.api.libs.json._
  
  object Keys {
    case object Id extends ModelKey("_id")
    case object LoginInfo extends ModelKey("loginInfo")
    case object FirstName extends ModelKey("firstName")
    case object LastName extends ModelKey("lastName")
    case object FullName extends ModelKey("fullName")
    case object Email extends ModelKey("email")
    case object AvatarURL extends ModelKey("avatarURL")
    case object ActiveOrganisation extends ModelKey("activeOrganisation")
  }

  implicit object UserWrites extends OWrites[User] {
      def writes(user: User): JsObject = {
        val sequence = Seq[JsField]() ++
          user.id.map(Keys.Id.value -> JsString(_)) ++
          user.loginInfo.map(Keys.LoginInfo.value -> Json.toJson(_)) ++
          user.firstName.map(Keys.FirstName.value -> JsString(_)) ++
          user.lastName.map(Keys.LastName.value -> JsString(_)) ++
          user.fullName.map(Keys.FullName.value -> JsString(_)) ++
          user.email.map(Keys.Email.value -> JsString(_)) ++
          user.avatarURL.map(Keys.AvatarURL.value -> JsString(_)) ++
          user.activeOrganisation.map(Keys.ActiveOrganisation.value -> JsString(_))
          
        JsObject(sequence)
      }
  }

  implicit object UserReads extends Reads[User] {
    def reads(json: JsValue): JsResult[User] = json match {
      case obj: JsObject => try {
        val id = (obj \ Keys.Id.value).asOpt[String]
        val loginInfo = (obj \ Keys.LoginInfo.value).asOpt[LoginInfo]
        val firstName = (obj \ Keys.FirstName.value).asOpt[String]
        val lastName = (obj \ Keys.LastName.value).asOpt[String]
        val fullName = (obj \ Keys.FullName.value).asOpt[String]
        val email = (obj \ Keys.Email.value).asOpt[String]
        val avatarURL = (obj \ Keys.AvatarURL.value).asOpt[String]
        val activeOrganisation = (obj \ Keys.ActiveOrganisation.value).asOpt[String]

        JsSuccess(User(id, loginInfo, firstName, lastName, fullName, email, avatarURL))
        
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}