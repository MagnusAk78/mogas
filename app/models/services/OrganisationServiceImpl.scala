package models.services

import scala.concurrent.Future

import javax.inject.Inject
import models.daos.ModelDAO
import play.api.libs.json.JsObject
import models.User
import models.Organisation
import models.Organisation._
import scala.concurrent.ExecutionContext
import play.api.libs.json.Reads
import play.api.libs.json.OWrites

class OrganisationServiceImpl @Inject()(override val dao: ModelDAO, userService: UserService)
  (implicit val ec: ExecutionContext) extends OrganisationService {
  
  implicit val joWrites: OWrites[Organisation] = Organisation.organisationFormat 
  
  implicit val joReads: Reads[Organisation] = Organisation.organisationFormat
  
  override def remove(organisation: Organisation): Future[RemoveResult] = {
    val futureUsersWithThisActive = userService.count(User.activeOrganisationQuery(organisation.uuid))
    
    val responses = for {
      usersWithThisActive <- futureUsersWithThisActive
      result <- if(usersWithThisActive == 0) {
        dao.remove(organisation.uuidQuery).map(success => if(success) { 
            RemoveResult(true, None)
        } else {
           RemoveResult(false, Some("DAO refused to remove organisation: " + organisation.uuid))
        }
        )
      } else {
        Future.successful(RemoveResult(false, Some("Users with this organisation active, uuid: " + organisation.uuid)))
      }
    } yield result
    
    responses recover {
        case e => RemoveResult(false, Some(e.getMessage()))
      }
  }
  
  
}