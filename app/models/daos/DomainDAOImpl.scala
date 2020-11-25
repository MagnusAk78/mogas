package models.daos

import javax.inject.Inject
import models.Domain
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, QueryOpts}
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class DomainDAOImpl @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit ec: ExecutionContext)
  extends DomainDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("domains"))

  override def insert(domain: Domain): Future[WriteResult] = collection.flatMap(_.insert.one(domain))

  override def update(document: Domain): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(Domain.queryByUuid(document.uuid), document)
  })

  override def remove(document: Domain): Future[Boolean] =
    collection.flatMap(_.delete().one(Domain.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Domain]] = collection.flatMap(
    _.find(query, None).cursor[Domain]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Domain]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Domain]().
      collect[List](pageSize, Cursor.FailOnError[List[Domain]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Domain]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Domain]().
      collect[List](pageSize, Cursor.FailOnError[List[Domain]]()))
}