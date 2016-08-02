package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.Factory

object FactoryForm {
    
  val form = Form[Data](
    mapping(
      "name" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(name: String)
   
  implicit def fromFactoryToData(factory: Factory): Data = Data(name = factory.name)
}