package models

import java.util.UUID
import play.api.libs.json._

case class IssueUpdate(
  override val uuid: String,
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String,
  val created: Long,
  val priority: Int,
  val closed: Boolean) extends DbModel with OrderedModel with ChildOf[Issue] with CreatedBy
    with HasText {

  override def asJsObject: JsObject = {
    IssueUpdate.orderedModelJsObject(this) ++
      IssueUpdate.childOfJsObject(this) ++
      IssueUpdate.createdByJsObject(this) ++
      IssueUpdate.hasTextJsObject(this) ++
      Json.obj(IssueUpdate.KeyCreated -> JsNumber(created)) ++
      Json.obj(IssueUpdate.KeyPriority -> JsNumber(priority)) ++
      Json.obj(IssueUpdate.KeyClosed -> JsBoolean(closed))
  }
}

object IssueUpdate extends DbModelComp[IssueUpdate] with ChildOfComp[Issue] with CreatedByComp
    with HasTextComp with OrderedModelComp {
  implicit val issueUpdateFormat = Json.format[IssueUpdate]

  private val KeyCreated = "created"
  private val KeyPriority = "priority"
  private val KeyClosed = "closed"

  def create(orderNumber: Int, parentIssue: String, text: String, createdBy: String, created: Long,
             closed: Boolean, priority: Int) =
    IssueUpdate(uuid = UUID.randomUUID.toString, orderNumber = orderNumber, parent = parentIssue,
      createdBy = createdBy, text = text, created = created, priority = priority, closed = closed)
}
