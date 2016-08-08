package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.Domain

object DomainForm {
    
  val form = Form[Data](
    mapping(
      "name" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(name: String)
   
  implicit def fromDomainToData(domain: Domain): Data = Data(name = domain.name)
}