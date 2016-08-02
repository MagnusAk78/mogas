package models.services

import utils.PaginateData
import models.Organisation
import models.daos.HierarchyDAO
import models.Hierarchy
import models.Factory
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json
import models.ChildOf

class HierarchyServiceImpl @Inject() (override val dao: HierarchyDAO)(implicit val ec: ExecutionContext) extends HierarchyService {

  override def getHierarchyList(page: Int, factory: Factory): Future[ModelListData[Hierarchy]] = {
    findMany(Hierarchy.queryByParent(factory), page, utils.DefaultValues.DefaultPageLength)
  }
}