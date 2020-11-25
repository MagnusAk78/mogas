package models.daos

import javax.inject.Inject
import models.{DbModel, Element}
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

trait ElementDAO extends BaseModelDAO[Element]

class ElementDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
  extends ElementDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("elements"))

  override def insert(element: Element): Future[WriteResult] = collection.flatMap(_.insert.one(element))

  override def update(element: Element): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(DbModel.queryByUuid(element.uuid), element)
  })

  override def remove(element: Element): Future[Boolean] =
    collection.flatMap(_.delete().one(DbModel.queryByUuid(element.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Element]] = collection.flatMap(
    _.find(query, None).cursor[Element]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Element]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Element]().
      collect[List](pageSize, Cursor.FailOnError[List[Element]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Element]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Element]().
      collect[List](pageSize, Cursor.FailOnError[List[Element]]()))
}