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
import models.FactoryPart

class HierarchyServiceImpl @Inject() (override val dao: HierarchyDAO)(implicit val ec: ExecutionContext) extends HierarchyService {

  override def getHierarchyList(page: Int, factory: Factory): Future[ModelListData[Hierarchy]] = {
    for {
      hierarchyList <- find(FactoryPart.queryByFactory(factory), page, utils.DefaultValues.DefaultPageLength)
      hierarchyCount <- count(FactoryPart.queryByFactory(factory))
    } yield new ModelListData[Hierarchy] {
      override val list = hierarchyList
      override val paginateData = PaginateData(page, hierarchyCount)
    }
  }
}