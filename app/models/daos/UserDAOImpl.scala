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

  override def find(user: User, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).cursor[User]().collect[List](maxDocs))

  override def findAndSort(user: User, sortBy: ModelKey, ascending: Boolean, maxDocs: Int = 0): Future[List[User]] =
    collection.flatMap(_.find(user).sort(DaoHelper.getSortByJsObject(sortBy.value, ascending)).cursor[User]().collect[List](maxDocs))
}
