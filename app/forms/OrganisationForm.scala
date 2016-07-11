package forms

import play.api.data.Form
import play.api.data.Forms._
import models.Organisation

object OrganisationForm {
    
  val form = Form[Data](
    mapping(
      "uuid" -> optional(nonEmptyText),
      "name" -> optional(nonEmptyText)
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(
    uuid: Option[String],
    name: Option[String])
    
  implicit def fromOrganisationToData(organisation: Organisation): Data = Data(uuid=organisation.uuid,
      name = organisation.name)
      
  implicit def fromDataToOrganisation(data: Data): Organisation = Organisation(uuid=data.uuid,
    name = data.name)
}