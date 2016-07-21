package models.services

import scala.concurrent.Future
import models.Hierarchy
import utils.PaginateData
import models.Factory

trait HierarchyService extends BaseModelService[Hierarchy] {

  def getHierarchyList(page: Int, factory: Factory): Future[ModelListData[Hierarchy]]
}