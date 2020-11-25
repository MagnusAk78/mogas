package models.daos

import javax.inject.Inject
import models.{DbModel, IssueUpdate}
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

trait IssueUpdateDAO extends BaseModelDAO[IssueUpdate]

class IssueUpdateDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
  extends IssueUpdateDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("issueUpdates"))

  override def insert(document: IssueUpdate): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: IssueUpdate): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(DbModel.queryByUuid(document.uuid), document)
  })

  override def remove(document: IssueUpdate): Future[Boolean] =
    collection.flatMap(_.delete().one(DbModel.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[IssueUpdate]] = collection.flatMap(
    _.find(query, None).cursor[IssueUpdate]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[IssueUpdate]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[IssueUpdate]().
      collect[List](pageSize, Cursor.FailOnError[List[IssueUpdate]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[IssueUpdate]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[IssueUpdate]().
      collect[List](pageSize, Cursor.FailOnError[List[IssueUpdate]]()))
}