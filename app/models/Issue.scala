package models

import java.util.UUID
import play.api.libs.json._
import java.util.Date

case class Issue(
    override val uuid: String,
    override val name: String,
    override val connectionTo: String,
    override val parent: String) extends DbModel with NamedModel with ConnectionTo[Domain] with ChildOf[AmlObject] {

  override def asJsObject: JsObject = {
    Issue.connectionToJsObject(this) ++
      Issue.namedModelJsObject(this) ++
      Issue.childOfJsObject(this)
  }
}

object Issue extends DbModelComp[Issue] with ChildOfComp[AmlObject] with ConnectionToComp[Domain]
    with CreatedByComp with NamedModelComp {
  implicit val issueFormat = Json.format[Issue]

  private val KeyCreatedByUser = "createdByUser"

  def create(name: String, connectionToToDomain: String, parentAmlObject: String, createdBy: String) =
    Issue(uuid = UUID.randomUUID.toString, name = name, connectionTo = connectionToToDomain,
      parent = parentAmlObject)
}

