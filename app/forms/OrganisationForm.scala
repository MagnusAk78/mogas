package forms

import play.api.data.Form
import play.api.data.Forms._
import models.Organisation

object OrganisationForm {
    
  val form = Form[Data](
    mapping(
      "uuid" -> optional(nonEmptyText),
      "name" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(
    uuid: Option[String],
    name: String)
   
  implicit def fromOrganisationToData(organisation: Organisation): Data = Data(uuid=Some(organisation.uuid),
      name = organisation.name)
   
      /*
  implicit def fromDataToOrganisation(data: Data): Organisation = data.uuid match {
    case Some(uuid) => Organisation(uuid=uuid, name=data.name, allowedUsers = data.allowedUsers, imageReadFileId = data.imageReadFileId)
    case None => Organisation.create(name=data.name, allowedUsers = data.allowedUsers)    
  }
  * */
}