package models.services

import scala.concurrent.Future
import models.InternalElement
import utils.PaginateData

trait InternalElementService extends BaseModelService[InternalElement] {

  def getInternalElementList(page: Int, parent: String): Future[ModelListData[InternalElement]]
}