package models.services

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import utils.ModelListData

import models.User
import play.api.libs.json.JsObject

/**
 * Handles actions to users.
 */
trait UserService extends IdentityService[User] {

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[Option[User]]

  def getUserList(page: Int, uuidSet: Set[String]): Future[ModelListData[User]]

  def insert(model: User): Future[Option[User]]

  def update(model: User): Future[Boolean]

  def findOne(query: JsObject): Future[Option[User]]

  def findMany(query: JsObject, page: Int = 1,
               pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[User]]
}