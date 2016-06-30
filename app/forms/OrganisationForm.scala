package forms

import play.api.data.Form
import play.api.data.Forms._
import models.Organisation

object OrganisationForm {
  
  val form = Form(
    mapping(
      "id" -> optional(nonEmptyText),
      "name" -> optional(nonEmptyText)
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(
    id: Option[String],
    name: Option[String])
}