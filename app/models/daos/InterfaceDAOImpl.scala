package models.daos

import javax.inject.Inject
import models.Interface
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class InterfaceDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends InterfaceDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("interfaces"))

  override def insert(document: Interface): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: Interface): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(Interface.queryByUuid(document.uuid), document)
  })

  override def remove(document: Interface): Future[Boolean] =
    collection.flatMap(_.delete().one(Interface.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Interface]] = collection.flatMap(
    _.find(query, None).cursor[Interface]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Interface]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Interface]().
      collect[List](pageSize, Cursor.FailOnError[List[Interface]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Interface]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Interface]().
      collect[List](pageSize, Cursor.FailOnError[List[Interface]]()))
}