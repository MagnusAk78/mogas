package models

import java.util.UUID
import play.api.libs.json.Json

case class IssueUpdate(
  override val uuid: String,
  override val modelType: String,
  override val orderNumber: Int,
  override val parent: String,
  override val createdBy: String,
  override val text: String,
  val created: Long,
  val priority: Int,
  val closed: Boolean) extends DbModel with HasModelType with OrderedModel with HasParent with HasCreatedBy
    with HasText

object IssueUpdate extends DbModelComp[IssueUpdate] with HasModelTypeComp with HasParentComp[Issue] with HasCreatedByComp
    with HasTextComp with OrderedModelComp {

  implicit val issueUpdateFormat = Json.format[IssueUpdate]

  private val KeyCreated = "created"
  private val KeyPriority = "priority"
  private val KeyClosed = "closed"

  def create(orderNumber: Int, parentIssue: String, text: String, createdBy: String, created: Long,
             closed: Boolean, priority: Int) =
    IssueUpdate(uuid = UUID.randomUUID.toString, modelType = Types.IssueUpdateType.stringValue,
        orderNumber = orderNumber, parent = parentIssue, createdBy = createdBy, text = text, 
        created = created, priority = priority, closed = closed)
}

