package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

import scala.collection.mutable.Map

object UserKeys extends Enumeration {
   val id = Value("_id")
   val loginInfo = Value("loginInfo")
   val firstName = Value("firstName")
   val lastName = Value("lastName")
   val fullName = Value("fullName")
   val email = Value("email")
   val avatarURL = Value("avatarURL")
} 

case class User(
    id: Option[String] = None,
    loginInfo: Option[LoginInfo] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    fullName: Option[String] = None,
    email: Option[String] = None,
    avatarURL: Option[String] = None) extends Identity
    
object User {
  import play.api.libs.json._
  
  type JsField = (String, JsValue)

  implicit object UserWrites extends OWrites[User] {
      def writes(user: User): JsObject = {
        val sequence = Seq[JsField]() ++
          user.id.map(id => UserKeys.id.toString -> JsString(id)) ++
          user.loginInfo.map(li => UserKeys.loginInfo.toString -> Json.toJson(li)) ++
          user.firstName.map(fn => UserKeys.firstName.toString -> JsString(fn)) ++
          user.lastName.map(ln => UserKeys.lastName.toString -> JsString(ln)) ++
          user.fullName.map(fn => UserKeys.fullName.toString -> JsString(fn)) ++
          user.email.map(email => UserKeys.email.toString -> JsString(email)) ++
          user.avatarURL.map(url => UserKeys.avatarURL.toString -> JsString(url))
          
        JsObject(sequence)
      }
  }

  implicit object UserReads extends Reads[User] {
    def reads(json: JsValue): JsResult[User] = json match {
      case obj: JsObject => try {
        val id = (obj \ UserKeys.id.toString).asOpt[String]
        val loginInfo = (obj \ UserKeys.loginInfo.toString).asOpt[LoginInfo]
        val firstName = (obj \ UserKeys.firstName.toString).asOpt[String]
        val lastName = (obj \ UserKeys.lastName.toString).asOpt[String]
        val fullName = (obj \ UserKeys.fullName.toString).asOpt[String]
        val email = (obj \ UserKeys.email.toString).asOpt[String]
        val avatarURL = (obj \ UserKeys.avatarURL.toString).asOpt[String]

        JsSuccess(User(id, loginInfo, firstName, lastName, fullName, email, avatarURL))
        
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }
}