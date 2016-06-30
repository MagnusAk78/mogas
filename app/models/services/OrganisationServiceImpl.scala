package models.services

import scala.concurrent.Future

import javax.inject.Inject
import models.daos.OrganisationDAO

class OrganisationServiceImpl @Inject()(override val dao: OrganisationDAO) extends OrganisationService {

}