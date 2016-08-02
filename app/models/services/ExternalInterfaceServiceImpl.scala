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
import models.AmlObject
import models.ChildOf

class ExternalInterfaceServiceImpl @Inject() (override val dao: ExternalInterfaceDAO)(implicit val ec: ExecutionContext)
    extends ExternalInterfaceService {

  def getExternalInterfaceList(page: Int, parent: InternalElement): Future[ModelListData[ExternalInterface]] = {
    findMany(ExternalInterface.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)
  }
}