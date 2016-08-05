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
import utils.RemoveResult
import utils.ModelListData

class OrganisationServiceImpl @Inject() (val dao: OrganisationDAO,
                                         userService: UserService)(implicit val ec: ExecutionContext) extends OrganisationService {

  override def remove(organisation: Organisation, loggedInUserUuid: String): Future[RemoveResult] = {
    if (organisation.allowedUsers.size != 1 || organisation.allowedUsers.headOption.get != loggedInUserUuid) {
      Future.successful(RemoveResult(false, Some("Remove all other users from the organisation first")))
    } else {
      val responses = for {
        anyUserWithThisActive <- userService.findOne(User.queryByActiveOrganisation(organisation.uuid))
        result <- if (anyUserWithThisActive.isEmpty) {
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

  override def insert(model: Organisation): Future[Option[Organisation]] = dao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def update(model: Organisation): Future[Boolean] = dao.update(model).map(wr => wr.ok)

  override def findOne(query: JsObject): Future[Option[Organisation]] = dao.find(query, 1, 1).map(_.headOption)

  override def findMany(query: JsObject, page: Int = 1,
                        pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Organisation]] = {
    for {
      theList <- dao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- dao.count(query)
    } yield new ModelListData[Organisation] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }
}