package models.services

import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.User

import scala.concurrent.Future
import models.daos.UserDAO

/**
 * Handles actions to users.
 */
trait UserService extends BaseModelService[User, UserDAO] with IdentityService[User] {

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[Option[User]]

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[Option[User]]
}