package forms

import play.api.data.Form
import play.api.data.Forms._
import models.User

object UserForm {
  val form = Form[Data](
    mapping(
      "uuid" -> optional(nonEmptyText),
      "firstName" -> optional(nonEmptyText),
      "lastName" -> optional(nonEmptyText),
      "email" -> optional(email)
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(
    uuid: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    email: Option[String]) {
    
    def getFullName: Option[String] = firstName match {
      case Some(fn) => lastName match {
          case Some(ln) => Some(fn + " " + ln)
          case None => Some(fn)
      }
      case None => lastName match {
          case Some(ln) => Some(ln)
          case None => None
      }
    }
  }
    
  implicit def fromUserToData(user: User): Data = Data(uuid=user.uuid,
      firstName = user.firstName, lastName = user.lastName, email = user.email)
      
  implicit def fromDataToUser(data: Data): User = User(uuid=data.uuid,
    firstName = data.firstName, lastName = data.lastName, fullName = data.getFullName,
    email = data.email)
}