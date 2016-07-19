package models.services

import scala.concurrent.Future
import models.Organisation
import utils.PaginateData

trait OrganisationService extends BaseModelService[Organisation] {
  
  def getOrganisationList(page: Int, allowedUserUuid: String): Future[ModelListData[Organisation]]
  
  def remove(organisation: Organisation, loggedInUserUuid: String): Future[RemoveResult]
}