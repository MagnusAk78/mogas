package models

import java.util.UUID
import play.api.libs.json._

case class Hierarchy(
  override val uuid: String,
  override val modelType: String,
  override val parent: String,
  override val name: String,
  override val orderNumber: Int,
  override val elements: Set[String]) extends DbModel with JsonImpl with HasModelType with 
    HasParent with HasElements with HasName with OrderedModel {

  override def asJsObject: JsObject = {
    Hierarchy.hasModelTypeJsObject(this) ++
    Hierarchy.childOfJsObject(this) ++
      Hierarchy.hasElementsJsObject(this) ++
      Hierarchy.namedModelJsObject(this) ++ Hierarchy.orderedModelJsObject(this)
  }
}

object Hierarchy extends DbModelComp[Hierarchy] with HasModelTypeComp with HasParentComp[Domain] with HasElementsComp
    with HasNameComp with OrderedModelComp {
  implicit val hierarchyFormat = Json.format[Hierarchy]

  def create(name: String, parentDomain: String, orderNumber: Int, elements: Set[String] = Set.empty) =
    Hierarchy(uuid = UUID.randomUUID.toString, modelType=Types.HierarchyType.stringValue, name = name, 
        parent = parentDomain, orderNumber = orderNumber, elements = elements)
}

/*
object HierarchyDbHelper {
  def getDomainHierarchies(domainId: ObjectId): Option[SalatMongoCursor[Hierarchy]] = {
    DomainDAO.findOneById(domainId) match {
      case Some(domain) => {
        Some(HierarchyDAO.find(DbHelper.queryIdFromIdList(domain.domainHierachies)).
          sort(DbHelper.sortAscKey(NumericlyOrdered.orderNumberKey)))
      }
      case None => None
    }
  }
}

object Hierarchy {

  val domainKey = "domain"

  private def identifyObjectType(objectIdString: String):
  Option[Either[Element, Interface]] = {
    ElementDAO.findOneById(objectIdString) match {
      case Some(element) => Some(Left(element))
      case None => InterfaceDAO.findOneById(objectIdString).map(Right(_))
    }
  }

  def getElementFullHierarchyFromObjectIdString(objectIdString: String): String = {

    var nameList = List.empty[String]
    var currentIdString: Option[String] = Some(objectIdString)

    while (currentIdString.isDefined) {
      identifyObjectType(currentIdString.get).fold()(_.fold(
        (element) => {
          //Add the element name to name list
          nameList = element.name :: nameList

          //If the parent is an hierarchy, add that name also to name list
          nameList = element.parentHierarchy
            .map(HierarchyDAO.findOneById(_)
              .map(_.name :: nameList).getOrElse(nameList))
            .getOrElse(nameList)

          //Set the next parent id if it is an internal element
          currentIdString = element.parentElement.map(_.toString)
        },
        (interface) => {
          //Add the interface name to name list
          nameList = interface.name :: nameList
          //Set the next parent id
          currentIdString = Some(interface.parentElement.toString)
        }))
    }

    nameList.tail.fold(nameList.head)((l: String, r: String) => l + " / " + r)
  }
}
*/ 