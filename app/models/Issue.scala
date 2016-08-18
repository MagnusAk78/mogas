package models

import java.util.UUID
import play.api.libs.json._
import java.util.Date

case class Issue(
    override val uuid: String,
    override val modelType: String,
    override val name: String,
    override val connectionTo: String,
    override val parent: String) extends DbModel with JsonImpl with HasModelType with HasName 
    with HasConnectionTo with HasParent {

  override def asJsObject: JsObject = {
    Issue.hasModelTypeJsObject(this) ++
    Issue.connectionToJsObject(this) ++
      Issue.namedModelJsObject(this) ++
      Issue.childOfJsObject(this)
  }
}

object Issue extends DbModelComp[Issue] with HasModelTypeComp 
    with HasParentComp[DbModel with HasAmlId] with HasConnectionToComp[Domain]
    with HasCreatedByComp with HasNameComp {
  implicit val issueFormat = Json.format[Issue]

  private val KeyCreatedByUser = "createdByUser"

  def create(name: String, connectionToToDomain: String, parentAmlObject: String, createdBy: String) =
    Issue(uuid = UUID.randomUUID.toString, modelType=Types.IssueType.stringValue, name = name, 
        connectionTo = connectionToToDomain, parent = parentAmlObject)
}

