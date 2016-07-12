package models.daos

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import javax.inject.Inject
import models.User
import play.api.Logger
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.api.QueryOpts

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends UserDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))

  override def insert(user: User): Future[Option[User]] =
    collection.flatMap { collection => collection.insert(user) }.map { wr => if(wr.ok) Some(user) else None }
  
  override def update(query: JsObject, update: JsObject): Future[Boolean] = collection.flatMap ( collection => collection.update(query, update)).map(wr => wr.ok)

  override def remove(query: JsObject): Future[Boolean] = collection.flatMap(_.remove(query).map { wr => wr.ok })

  override def count(query: JsObject): Future[Int] = collection.flatMap(_.count(Some(query)))

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[User]] =
    collection.flatMap(_.find(query).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[User]().collect[List](pageSize))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[User]] =
    collection.flatMap(_.find(query).sort(sort).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[User]().collect[List](pageSize))
       
}
