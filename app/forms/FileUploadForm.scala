package forms

import play.api.data.Form
import play.api.data.Forms._

object FileUploadForm {
    
  val form = Form[Data](
    mapping(
      "file" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
    
  case class Data(file: String)
}