package models.services

import scala.concurrent.Future

import javax.inject.Inject
import models.daos.OrganisationDAO
import play.api.libs.json.JsObject
import models.User
import models.Organisation
import models.Organisation._
import scala.concurrent.ExecutionContext
import play.api.libs.json.Reads
import play.api.libs.json.OWrites
import utils.PaginateData

class OrganisationServiceImpl @Inject() (override val dao: OrganisationDAO, userService: UserService)(implicit val ec: ExecutionContext) extends OrganisationService {

  override def remove(organisation: Organisation, loggedInUserUuid: String): Future[RemoveResult] = {
    if (organisation.allowedUsers.size != 1 || organisation.allowedUsers.headOption.get != loggedInUserUuid) {
      Future.successful(RemoveResult(false, Some("Remove all other users from the organisation first")))
    } else {
      val responses = for {
        usersWithThisActive <- userService.count(User.queryByActiveOrganisation(organisation.uuid))
        result <- if (usersWithThisActive == 0) {
          dao.remove(organisation).map(success => if (success) {
            RemoveResult(true, None)
          } else {
            RemoveResult(false, Some("DAO refused to remove organisation: " + organisation.uuid))
          })
        } else {
          Future.successful(RemoveResult(false, Some("Users with this organisation active, uuid: " + organisation.uuid)))
        }
      } yield result

      responses recover {
        case e => RemoveResult(false, Some(e.getMessage()))
      }
    }
  }

  override def getOrganisationList(page: Int, allowedUserUuid: String): Future[ModelListData[Organisation]] = {
    val selector = Organisation.queryByAllowedUser(allowedUserUuid)
    findMany(selector, page, utils.DefaultValues.DefaultPageLength)
  }
}