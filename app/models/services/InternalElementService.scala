package models.services

import scala.concurrent.Future
import models.InternalElement
import utils.PaginateData
import models.InternalElementParent
import models.ExternalInterface
import models.Hierarchy

trait InternalElementService extends BaseModelService[InternalElement] {

  def getInternalElementList(page: Int, parent: InternalElementParent): Future[ModelListData[InternalElement]]

  def getElementChain(uuid: String): Future[List[InternalElement]]
}