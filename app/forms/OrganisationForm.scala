package forms

import play.api.data.Form
import play.api.data.Forms._
import models.Organisation

object OrganisationForm {
    
  val form = Form[Data](
    mapping(
      "name" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(
    name: String)
   
  implicit def fromOrganisationToData(organisation: Organisation): Data = Data(name = organisation.name)
}