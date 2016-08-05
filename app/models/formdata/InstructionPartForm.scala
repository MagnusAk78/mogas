package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.InstructionPart

object InstructionPartForm {

  val form = Form[Data](
    mapping(
      "text" -> nonEmptyText)(Data.apply)(Data.unapply))

  case class Data(text: String)

  implicit def fromInstructionPartToData(instructionPart: InstructionPart): Data = Data(text = instructionPart.text)
}