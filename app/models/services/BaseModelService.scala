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
import models.DbModelComp
import utils.PaginateData

trait BaseModelService[M <: DbModel] {

  implicit val ec: ExecutionContext

  protected val dao: BaseModelDAO[M]

  final def insert(model: M): Future[Option[M]] = dao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  final def update(model: M): Future[Boolean] = dao.update(model).map(wr => wr.ok)

  final def count(query: JsObject): Future[Int] = dao.count(query)

  final def findOneByUuid(uuid: String): Future[Option[M]] = dao.findOneByUuid(uuid)

  final def findOne(query: JsObject): Future[Option[M]] = dao.find(query, 1, 1).map(_.headOption)

  final def findMany(query: JsObject, page: Int = 1,
                     pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[M]] = {
    for {
      theList <- dao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- count(query)
    } yield new ModelListData[M] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }

  final def findManySorted(query: JsObject, sort: JsObject, page: Int = 1,
                           pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[M]] = {
    for {
      theList <- dao.findAndSort(query, sort, page, utils.DefaultValues.DefaultPageLength)
      count <- count(query)
    } yield new ModelListData[M] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }
}