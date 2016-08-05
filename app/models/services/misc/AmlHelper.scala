package models.services.misc

import java.io.InputStream

trait NumericlyOrdered extends Ordered[NumericlyOrdered] {
  val orderNumber: Int

  def compare(that: NumericlyOrdered) = this.orderNumber - that.orderNumber
}

object NumericlyOrdered {
  val orderNumberKey = "orderNumber"
}

case class AmlHierarchy(
  name: String,
  orderNumber: Int,
  elements: List[AmlElement]) extends NumericlyOrdered

case class AmlElement(
  name: String,
  orderNumber: Int,
  amlId: String,
  elements: List[AmlElement],
  interfaces: List[AmlInterface]) extends NumericlyOrdered

case class AmlInterface(
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
        AmlHierarchy(name, hierarchyNumbers.next, findElements(instance))
      }).toList
  }

  private def findElements(node: Node): List[AmlElement] = {
    val elementNumbers = Stream.iterate(0)(_ + 1).iterator
    (node \ "InternalElement")
      .map(ie => {
        val name = (ie \ "@Name").text
        val amlId = (ie \ "@ID").text
        AmlElement(name, elementNumbers.next, amlId, findElements(ie), findInterfaces(ie))
      }).toList
  }

  private def findInterfaces(node: Node): List[AmlInterface] = {
    val interfaceNumbers = Stream.iterate(0)(_ + 1).iterator
    (node \ "ExternalInterface")
      .map(ei => AmlInterface((ei \ "@Name").text, interfaceNumbers.next, (ei \ "@ID").text)).toList
  }
}

