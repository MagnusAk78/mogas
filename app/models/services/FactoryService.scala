package models.services

import scala.concurrent.Future
import models.Factory
import models.Hierarchy
import utils.PaginateData
import models.Organisation
import play.api.libs.json.JsObject
import utils.RemoveResult
import utils.ModelListData
import utils.AmlObjectChain
import models.ChildOf
import models.AmlObject

trait FactoryService {

  def getFactoryList(page: Int, activeOrganisation: Organisation): Future[ModelListData[Factory]]

  def removeFactory(factory: Factory, loggedInUserUuid: String): Future[RemoveResult]

  def parseAmlFiles(factory: Factory): Future[Boolean]

  def insertFactory(model: Factory): Future[Option[Factory]]

  def updateFactory(model: Factory): Future[Boolean]

  def findOneFactory(query: JsObject): Future[Option[Factory]]

  def findManyFactories(query: JsObject, page: Int = 1,
                        pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Factory]]

  def getHierarchyList(page: Int, factory: Factory): Future[ModelListData[Hierarchy]]

  def insertHierarchy(model: Hierarchy): Future[Option[Hierarchy]]

  def updateHierarchy(model: Hierarchy): Future[Boolean]

  def findOneHierarchy(query: JsObject): Future[Option[Hierarchy]]

  def findManyHierarchies(query: JsObject, page: Int = 1,
                          pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Hierarchy]]

  def getAmlObjectChains(children: List[ChildOf[AmlObject]]): Future[List[AmlObjectChain]]
}