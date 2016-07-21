package models.services

import models.ExternalInterface
import scala.concurrent.Future

trait ExternalInterfaceService extends BaseModelService[ExternalInterface] {

  def getExternalInterfaceList(page: Int, parentInternalElement: String): Future[ModelListData[ExternalInterface]]
}