package models.services

import utils.PaginateData
import models.Organisation
import models.daos.ExternalInterfaceDAO
import models.Factory
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json
import models.InternalElement
import models.ExternalInterface
import models.HierarchyPart

class ExternalInterfaceServiceImpl @Inject() (override val dao: ExternalInterfaceDAO)(implicit val ec: ExecutionContext)
    extends ExternalInterfaceService {

  def getExternalInterfaceList(page: Int, parent: InternalElement): Future[ModelListData[ExternalInterface]] = {
    for {
      externalInterfaceList <- find(HierarchyPart.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)
      externalInterfaceCount <- count(HierarchyPart.queryByParent(parent))
    } yield new ModelListData[ExternalInterface] {
      override val list = externalInterfaceList
      override val paginateData = PaginateData(page, externalInterfaceCount)
    }
  }
}