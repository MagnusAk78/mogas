package viewdata

import models.Domain
import models.daos.FileDAO.JSONReadFile

case class DomainData(domain: Domain, imageExists: Boolean, amlFiles: List[JSONReadFile])