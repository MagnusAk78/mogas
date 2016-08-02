package models.services

import scala.concurrent.Future
import models.Factory
import utils.PaginateData
import models.Organisation

trait FactoryService extends BaseModelService[Factory] {

  def getFactoryList(page: Int, activeOrganisation: Organisation): Future[ModelListData[Factory]]

  def remove(factory: Factory, loggedInUserUuid: String): Future[RemoveResult]

  def parseAmlFiles(factory: Factory): Future[Boolean]
}