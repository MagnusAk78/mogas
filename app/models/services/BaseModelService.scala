package models.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.DbModel
import models.daos.BaseModelDAO
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import models.DbModel

trait BaseModelService[M <: DbModel] {

  implicit val ec: ExecutionContext

  protected val dao: BaseModelDAO[M]

  final def insert(model: M): Future[Option[M]] = dao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  final def update(model: M): Future[Boolean] = dao.update(model).map(wr => wr.ok)

  final def count(query: JsObject): Future[Int] = dao.count(query)

  final def findOneByUuid(uuid: String): Future[Option[M]] = findOne(DbModel.queryByUuid(uuid))

  final def findOne(query: JsObject): Future[Option[M]] = find(query, 1, 1).map(_.headOption)

  final def find(query: JsObject, page: Int = 1,
                 pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] = {

    Logger.info("BaseModelService.find query: " + query)
    Logger.info("BaseModelService.find page: " + page)
    Logger.info("BaseModelService.find pageSize: " + pageSize)

    dao.find(query, page, pageSize)
  }

  final def findAndSort(query: JsObject, sort: JsObject, page: Int = 1,
                        pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] =
    dao.findAndSort(query, sort, page, pageSize)
}