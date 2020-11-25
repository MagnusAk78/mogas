package models.daos

import javax.inject.Inject
import models.User
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends UserDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("users"))

  override def insert(document: User): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: User): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(User.queryByUuid(document.uuid), document)
  })

  override def remove(document: User): Future[Boolean] =
    collection.flatMap(_.delete().one(User.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[User]] = collection.flatMap(
    _.find(query, None).cursor[User]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[User]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[User]().
      collect[List](pageSize, Cursor.FailOnError[List[User]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[User]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[User]().
      collect[List](pageSize, Cursor.FailOnError[List[User]]()))
}