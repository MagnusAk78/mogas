package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.Issue

object IssueForm {

  val form = Form[Data](
    mapping(
      "name" -> nonEmptyText)(Data.apply)(Data.unapply))

  case class Data(name: String)

  implicit def fromIssueToData(instruction: Issue): Data = Data(name = instruction.name)
}