package models.services

import scala.concurrent.Future
import models.Organisation
import models.daos.OrganisationDAO

trait OrganisationService extends BaseModelService[Organisation, OrganisationDAO] {

}