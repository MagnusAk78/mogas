package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.User
import models.daos.UserDAO
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (override val dao: UserDAO) extends UserService {
 
  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = dao.find(User(loginInfo = Some(loginInfo))).map { _.headOption }


  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  override def save(profile: CommonSocialProfile): Future[Option[User]] = {
    retrieve(profile.loginInfo).flatMap { optionUser =>
      optionUser match {
        case Some(user) => { // Update user with profile
          val newUser = user.copy(
            id = user.id,
            loginInfo = Some(profile.loginInfo),
            firstName = profile.firstName,
            lastName = profile.lastName,
            fullName = profile.fullName,
            email = profile.email,
            avatarURL = profile.avatarURL)
          dao.save(newUser)
        }
        case None => { // Insert a new user
          val newUser = User(
            id = None,
            loginInfo = Some(profile.loginInfo),
            firstName = profile.firstName,
            lastName = profile.lastName,
            fullName = profile.fullName,
            email = profile.email,
            avatarURL = profile.avatarURL)
          dao.save(newUser)
        }
      }
    }
  }
}