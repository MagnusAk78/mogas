package models.services

import scala.concurrent.Future
import models.Organisation
import utils.PaginateData
import play.api.libs.json.JsObject
import utils.RemoveResult
import utils.ModelListData

trait OrganisationService {

  def getOrganisationList(page: Int, allowedUserUuid: String): Future[ModelListData[Organisation]]

  def remove(organisation: Organisation, loggedInUserUuid: String): Future[RemoveResult]

  def insert(model: Organisation): Future[Option[Organisation]]

  def update(model: Organisation): Future[Boolean]

  def findOne(query: JsObject): Future[Option[Organisation]]

  def findMany(query: JsObject, page: Int = 1,
               pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Organisation]]
}