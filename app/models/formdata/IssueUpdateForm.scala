package models.formdata

import play.api.data.Form
import play.api.data.Forms._
import models.IssueUpdate

object IssueUpdateForm {

  val form = Form[Data](
    mapping(
      "text" -> nonEmptyText)(Data.apply)(Data.unapply))

  case class Data(text: String)

  implicit def fromIssueUpdateToData(issueUpdate: IssueUpdate): Data = Data(text = issueUpdate.text)
}