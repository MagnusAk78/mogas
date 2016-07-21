package models.services

import utils.PaginateData
import models.Organisation
import models.daos.InternalElementDAO
import models.Hierarchy
import models.Factory
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json
import models.InternalElement
import models.HierarchyPart

class InternalElementServiceImpl @Inject() (override val dao: InternalElementDAO)(implicit val ec: ExecutionContext) extends InternalElementService {

  override def getInternalElementList(page: Int, parent: String): Future[ModelListData[InternalElement]] = {
    for {
      internalElementList <- find(HierarchyPart.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)
      internalElementCount <- count(HierarchyPart.queryByParent(parent))
    } yield new ModelListData[InternalElement] {
      override val list = internalElementList
      override val paginateData = PaginateData(page, internalElementCount)
    }
  }
}