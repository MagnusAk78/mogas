package models

import java.util.UUID
import play.api.libs.json._

case class Element(
  override val uuid: String,
  override val modelType: String,
  override val connectionTo: String,
  override val amlId: String,
  override val parent: String,
  override val orderNumber: Int,
  override val name: String,
  override val elements: Set[String],
  parentIsHierarchy: Boolean,
  interfaces: Set[String]) extends DbModel with JsonImpl with HasModelType with AmlObject
    with ChildOf[ElementParent] with ElementParent {

  override def asJsObject: JsObject =
    Element.hasModelTypeJsObject(this) ++
    Element.amlObjectJsObject(this) ++
      Element.childOfJsObject(this) ++
      Element.elementParentJsObject(this) ++
      Json.obj(Element.KeyParentIsHierarchy -> JsBoolean(parentIsHierarchy),
        Element.KeyInterfaces -> Json.toJson(interfaces))
}

object Element extends DbModelComp[Element] with HasModelTypeComp with AmlObjectComp with ChildOfComp[ElementParent]
    with ElementParentComp {

  implicit val elementFormat = Json.format[Element]

  private val KeyParentIsHierarchy = "parentIsHierarchy"
  private val KeyInterfaces = "interfaces"

  def create(connectionToDomain: String, name: String, parent: String, parentIsHierarchy: Boolean = false,
             orderNumber: Int, amlId: String, elements: Set[String] = Set.empty,
             interfaces: Set[String] = Set.empty) =
    Element(uuid = UUID.randomUUID.toString, modelType = Types.ElementType.stringValue, connectionTo = connectionToDomain, 
        name = name, parent = parent, parentIsHierarchy = parentIsHierarchy, orderNumber = orderNumber, amlId = amlId,
      elements = elements, interfaces = interfaces)
}

/* TODO: Integrate in Service

object ElementDbHelper {

  def getElements(element: Either[Hierarchy, Element]):
  Option[SalatMongoCursor[Element]] = element.fold(
    (hierarchy) => {
      Some(ElementDAO.find(DbHelper.queryIdFromIdList(hierarchy.elements)).
        sort(DbHelper.sortAscKey("orderNumber"/*NumericlyOrdered.orderNumberKey*/)))
    },
    (element) => {
      Some(ElementDAO.find(DbHelper.queryIdFromIdList(element.elements)).
        sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey)))
    }
  )

  def getElementParent(element: Element):
  Either[Option[Hierarchy], Option[Element]] = element.parentElement match {
    case Some(internalElmentObjectId) => Right(ElementDAO.findOneById(internalElmentObjectId))
    case None => element.parentHierarchy match {
      case Some(hierarchyObjectId) => Left(HierarchyDAO.findOneById(hierarchyObjectId))
      case None => Left(None)
    }
  }
}

object Element extends ImageHandler[Element] {

  def getElementChainList(element: Element): List[Element] = {

    @tailrec
    def getElementChainListInternal(element: Element, list: List[Element]):
    List[Element] = element.parentElement match {
      case Some(parentElementObjectId) =>
        ElementDAO.findOneById(parentElementObjectId) match {
          case Some(parentElement) => getElementChainListInternal(parentElement, parentElement :: list)
          case None => list
        }
      case None => list
    }

    getElementChainListInternal(element, List(element))
  }

  def cleanup(element: Element): Unit = {
    element.interfaces.foreach(
      (eiObjectId: ObjectId) => {
        InterfaceDAO.findOneById(eiObjectId) match {
          case Some(interface) => Interface.cleanup(interface)
          case None => Logger.info("interface not found" + eiObjectId)
        }
      }
    )

    element.elements.foreach(
      (ieObjectId: ObjectId) => {
        ElementDAO.findOneById(ieObjectId) match {
          case Some(element) => ElementDAO.remove(element)
          case None => Logger.info("element not found:" + ieObjectId)
        }
      }
    )

    ElementDAO.remove(element)
  }
}
*/ 