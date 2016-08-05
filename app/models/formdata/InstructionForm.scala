package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.Instruction

object InstructionForm {

  val form = Form[Data](
    mapping(
      "name" -> nonEmptyText)(Data.apply)(Data.unapply))

  case class Data(name: String)

  implicit def fromInstructionToData(instruction: Instruction): Data = Data(name = instruction.name)
}