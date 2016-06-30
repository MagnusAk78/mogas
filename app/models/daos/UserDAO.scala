package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.User

import scala.concurrent.Future
import models.UserKeys

/**
 * Give access to the user object.
 */
trait UserDAO {
  
  def save(user: User): Future[Option[User]]
  
  def remove(user: User): Future[Boolean]
  
  def find(user: User, maxDocs: Int = 0): Future[List[User]]
  
  def findAndSort(user: User, sortBy: UserKeys.Value, ascending: Boolean, maxDocs: Int = 0): Future[List[User]]
}