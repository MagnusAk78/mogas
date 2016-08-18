package models.services

import play.api.libs.json.JsObject
import scala.concurrent.Future
import models.HasAmlId
import models.Instruction
import models.Interface
import models.Element
import models.HasElements
import utils.ModelListData
import models.DbModel

trait AmlObjectService {

  def getElementList(page: Int, parent: DbModel with HasElements): Future[ModelListData[Element]]

  def insertElement(model: Element): Future[Option[Element]]

  def updateElement(model: Element): Future[Boolean]

  def findOneElement(query: JsObject): Future[Option[Element]]

  def getElementChain(uuid: String): Future[List[Element]]

  def findManyElements(query: JsObject, page: Int = 1,
                       pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Element]]

  def getInterfaceList(page: Int, parent: Element): Future[ModelListData[Interface]]

  def insertInterface(model: Interface): Future[Option[Interface]]

  def updateInterface(model: Interface): Future[Boolean]

  def findOneInterface(query: JsObject): Future[Option[Interface]]

  def findManyInterfaces(query: JsObject, page: Int = 1,
                         pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Interface]]
}