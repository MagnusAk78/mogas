package models.daos

import javax.inject.Inject
import models.Issue
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class IssueDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends IssueDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("issues"))

  override def insert(document: Issue): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: Issue): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(Issue.queryByUuid(document.uuid), document)
  })

  override def remove(document: Issue): Future[Boolean] =
    collection.flatMap(_.delete().one(Issue.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Issue]] = collection.flatMap(
    _.find(query, None).cursor[Issue]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Issue]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Issue]().
      collect[List](pageSize, Cursor.FailOnError[List[Issue]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Issue]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Issue]().
      collect[List](pageSize, Cursor.FailOnError[List[Issue]]()))
}