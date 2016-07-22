package models.services

import models.ExternalInterface
import scala.concurrent.Future
import models.InternalElement

trait ExternalInterfaceService extends BaseModelService[ExternalInterface] {

  def getExternalInterfaceList(page: Int, parent: InternalElement): Future[ModelListData[ExternalInterface]]
}