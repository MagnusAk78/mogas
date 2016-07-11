package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.User

import scala.collection.mutable
import scala.concurrent.Future
import play.modules.reactivemongo.ReactiveMongoApi
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.api.QueryOpts

import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends UserDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))

  override def insert(user: User): Future[Option[User]] =
    collection.flatMap { collection => collection.insert(user) }.map { wr => if(wr.ok) Some(user) else None }
  
  override def update(uuid: String, newUser: User): Future[Option[User]] = {
    
    val selector = JsObject(Seq(User.KeyUUID -> JsString(uuid)))
    
    val updateJsValue = JsObject(Seq("$set" -> User.UserWrites.writes(newUser)))
    
    collection.flatMap ( collection => 
      collection.update(selector, updateJsValue)).map(wr => if(wr.ok) Some(newUser) else None)
  }

  override def remove(user: User): Future[Boolean] = collection.flatMap(_.remove(user).map { wr => wr.ok })

  override def count(user: User): Future[Int] = collection.flatMap(_.count(Some(User.UserWrites.writes(user))))

  override def find(user: User, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[User]().collect[List](maxDocs))

  override def findAndSort(user: User, sortBy: String, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).sort(DaoHelper.getSortByJsObject(sortBy, ascending)).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[User]().collect[List](maxDocs))
       
}
