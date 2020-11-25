package models.daos

import javax.inject.Inject
import models.Hierarchy
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class HierarchyDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends HierarchyDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("hierarchies"))

  override def insert(document: Hierarchy): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: Hierarchy): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(Hierarchy.queryByUuid(document.uuid), document)
  })

  override def remove(document: Hierarchy): Future[Boolean] =
    collection.flatMap(_.delete().one(Hierarchy.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Hierarchy]] = collection.flatMap(
    _.find(query, None).cursor[Hierarchy]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Hierarchy]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Hierarchy]().
      collect[List](pageSize, Cursor.FailOnError[List[Hierarchy]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Hierarchy]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Hierarchy]().
      collect[List](pageSize, Cursor.FailOnError[List[Hierarchy]]()))
}