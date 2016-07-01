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
import models.ModelKey
import reactivemongo.api.QueryOpts


/**
 * Give access to the user object.
 */
class UserDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends UserDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))

  override def save(user: User): Future[Option[User]] = user.id match {
    case Some(id) => {
      //This has an id, it is probably an update
      val selector = User(id = Some(id))

      collection.flatMap(_.update(selector = selector, user).map { wr =>
        wr.ok match {
          case true  => Some(user)
          case false => None
        }
      })
    }
    case None => {
      //This has no id, it is an insert
      collection.flatMap(_.insert(user).map { wr =>
        wr.ok match {
          case true  => Some(user)
          case false => None
        }
      })
    }
  }

  override def remove(user: User): Future[Boolean] = collection.flatMap(_.remove(user).map { wr => wr.ok })
  
  import models.User.UserWrites
  
  override def count(user: User): Future[Int] = collection.flatMap(_.count(Some(User.UserWrites.writes(user))))

  override def find(user: User, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[User]().collect[List](maxDocs))

  override def findAndSort(user: User, sortBy: ModelKey, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).sort(DaoHelper.getSortByJsObject(sortBy.value, ascending)).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[User]().collect[List](maxDocs))
       
}
