package models

import java.util.UUID
import play.api.libs.json._

case class InternalElement(
    override val uuid: String,
    override val factory: String,
    override val amlId: String,
    override val parent: String,
    override val orderNumber: Int,
    override val name: String,
    parentIsHierarchy: Boolean,
    internalElements: Set[String],
    externalInterfaces: Set[String]) extends DbModel with FactoryPart with HierarchyPart with NamedModel with OrderedModel {

  override def asJsObject: JsObject =
    FactoryPart.asJsObject(this) ++
      HierarchyPart.asJsObject(this) ++
      NamedModel.asJsObject(this) ++
      OrderedModel.asJsObject(this) ++
      Json.obj(InternalElement.KeyParentIsHierarchy -> JsBoolean(parentIsHierarchy),
        InternalElement.KeyInternalElements -> Json.toJson(internalElements),
        InternalElement.KeyExternalInterfaces -> Json.toJson(externalInterfaces))
}

object InternalElement {

  implicit val internalElementFormat = Json.format[InternalElement]

  private val KeyParentIsHierarchy = "parentIsHierarchy"
  private val KeyInternalElements = "internalElements"
  private val KeyExternalInterfaces = "externalInterfaces"

  def create(factory: String, name: String, parent: String, parentIsHierarchy: Boolean = false, orderNumber: Int, amlId: String,
             internalElements: Set[String] = Set.empty, externalInterfaces: Set[String] = Set.empty) =
    InternalElement(uuid = UUID.randomUUID.toString, factory = factory, name = name, parent = parent, parentIsHierarchy = parentIsHierarchy,
      orderNumber = orderNumber, amlId = amlId, internalElements = internalElements,
      externalInterfaces = externalInterfaces)
}

/* TODO: Integrate in Service

object InternalElementDbHelper {

  def getInternalElements(element: Either[Hierarchy, InternalElement]):
  Option[SalatMongoCursor[InternalElement]] = element.fold(
    (hierarchy) => {
      Some(InternalElementDAO.find(DbHelper.queryIdFromIdList(hierarchy.internalElements)).
        sort(DbHelper.sortAscKey("orderNumber"/*NumericlyOrdered.orderNumberKey*/)))
    },
    (element) => {
      Some(InternalElementDAO.find(DbHelper.queryIdFromIdList(element.internalElements)).
        sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey)))
    }
  )

  def getInternalElementParent(element: InternalElement):
  Either[Option[Hierarchy], Option[InternalElement]] = element.parentInternalElement match {
    case Some(internalElmentObjectId) => Right(InternalElementDAO.findOneById(internalElmentObjectId))
    case None => element.parentHierarchy match {
      case Some(hierarchyObjectId) => Left(HierarchyDAO.findOneById(hierarchyObjectId))
      case None => Left(None)
    }
  }
}

object InternalElement extends ImageHandler[InternalElement] {

  def getElementChainList(internalElement: InternalElement): List[InternalElement] = {

    @tailrec
    def getElementChainListInternal(internalElement: InternalElement, list: List[InternalElement]):
    List[InternalElement] = internalElement.parentInternalElement match {
      case Some(parentInternalElementObjectId) =>
        InternalElementDAO.findOneById(parentInternalElementObjectId) match {
          case Some(parentElement) => getElementChainListInternal(parentElement, parentElement :: list)
          case None => list
        }
      case None => list
    }

    getElementChainListInternal(internalElement, List(internalElement))
  }

  def cleanup(internalElement: InternalElement): Unit = {
    internalElement.externalInterfaces.foreach(
      (eiObjectId: ObjectId) => {
        ExternalInterfaceDAO.findOneById(eiObjectId) match {
          case Some(externalInterface) => ExternalInterface.cleanup(externalInterface)
          case None => Logger.info("externalInterface not found" + eiObjectId)
        }
      }
    )

    internalElement.internalElements.foreach(
      (ieObjectId: ObjectId) => {
        InternalElementDAO.findOneById(ieObjectId) match {
          case Some(internalElement) => InternalElementDAO.remove(internalElement)
          case None => Logger.info("internalElement not found:" + ieObjectId)
        }
      }
    )

    InternalElementDAO.remove(internalElement)
  }
}
*/ 