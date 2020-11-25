package models.services

import models._
import play.api.libs.json.JsObject
import utils.RemoveResult
import viewdata.{AmlObjectData, ModelListData}

import scala.concurrent.Future

trait DomainService {

  def getDomainList(page: Int, allowedUser: User): Future[ModelListData[Domain]]

  def removeDomain(domain: Domain, loggedInUserUuid: String): Future[RemoveResult]

  def parseAmlFiles(domain: Domain): Future[Boolean]

  def insertDomain(model: Domain): Future[Option[Domain]]

  def updateDomain(model: Domain): Future[Boolean]

  def findOneDomain(query: JsObject): Future[Option[Domain]]

  def findManyDomains(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Domain]]

  def getHierarchyList(page: Int, domain: Domain): Future[ModelListData[Hierarchy]]

  def insertHierarchy(model: Hierarchy): Future[Option[Hierarchy]]

  def updateHierarchy(model: Hierarchy): Future[Boolean]

  def findOneHierarchy(query: JsObject): Future[Option[Hierarchy]]

  def findManyHierarchies(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Hierarchy]]

  def getAmlObjectDatas(children: List[DbModel with HasParent]): Future[List[AmlObjectData]]
}