package models.services

import java.io.InputStream
import play.api.Logger

case class AmlHierarchy(
  name: String,
  orderNumber: Int,
  internalElements: List[AmlInternalElement]) extends NumericlyOrdered

case class AmlInternalElement(
  name: String,
  orderNumber: Int,
  amlId: String,
  internalElements: List[AmlInternalElement],
  externalInterfaces: List[AmlExternalInterface]) extends NumericlyOrdered

case class AmlExternalInterface(
  name: String,
  orderNumber: Int,
  amlId: String) extends NumericlyOrdered

object AmlHelper {

  import scala.xml.{ Node, Elem }

  import scala.xml.parsing.ConstructingParser
  import scala.xml.pull._
  import scala.io.Source

  val amlIdKey = "amlId"

  def generateFromStream(amlStream: InputStream): List[AmlHierarchy] = {
    findInstanceHierarchies(scala.xml.XML.load(amlStream))
  }

  private def findInstanceHierarchies(aml: Node): List[AmlHierarchy] = {
    val hierarchyNumbers = Stream.iterate(0)(_ + 1).iterator
    (aml \ "InstanceHierarchy")
      .map(instance => {
        val name = (instance \ "@Name").text
        AmlHierarchy(name, hierarchyNumbers.next, findInternalElements(instance))
      }).toList
  }

  private def findInternalElements(node: Node): List[AmlInternalElement] = {
    val elementNumbers = Stream.iterate(0)(_ + 1).iterator
    (node \ "InternalElement")
      .map(ie => {
        val name = (ie \ "@Name").text
        val amlId = (ie \ "@ID").text
        AmlInternalElement(name, elementNumbers.next, amlId, findInternalElements(ie), findExternalInterfaces(ie))
      }).toList
  }

  private def findExternalInterfaces(node: Node): List[AmlExternalInterface] = {
    val interfaceNumbers = Stream.iterate(0)(_ + 1).iterator
    (node \ "ExternalInterface")
      .map(ei => AmlExternalInterface((ei \ "@Name").text, interfaceNumbers.next, (ei \ "@ID").text)).toList
  }
}

