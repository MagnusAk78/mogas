package models.services

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import models.User

import play.api.Logger

import scala.concurrent.Future
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext
import models.daos.UserDAO
import play.api.libs.json.Reads
import play.api.libs.json.OWrites
import utils.PaginateData
import models.DbModel

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (val dao: UserDAO, implicit override val ec: ExecutionContext) extends UserService {

  implicit val joWrites: OWrites[User] = User.userFormat

  implicit val joReads: Reads[User] = User.userFormat

  override def getUserList(page: Int, uuidSet: Set[String]): Future[ModelListData[User]] = {
    for {
      userList <- find(DbModel.queryBySetOfUuids(uuidSet), page, utils.DefaultValues.DefaultPageLength)
      userCount <- count(DbModel.queryBySetOfUuids(uuidSet))
    } yield new ModelListData[User] {
      override val list = userList
      override val paginateData = PaginateData(page, userCount)
    }
  }

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = find(User.queryByLoginInfo(loginInfo)).map { _.headOption }

  /*
  override def save(user: User): Future[Option[User]] = {

    Logger.info("UserServiceImpl save (user): " + user)

    user.loginInfo match {
      case Some(loginInfo) => {
        retrieve(loginInfo).flatMap { optionUser =>
          optionUser match {
            case Some(oldUser) => { // Update user with profile
              val updatedUser = User(
                loginInfo = user.loginInfo,
                firstName = user.firstName,
                lastName = user.lastName,
                fullName = user.fullName,
                email = user.email,
                avatarURL = user.avatarURL,
                activeOrganisation = user.activeOrganisation)
              dao.update(oldUser.uuid.get, updatedUser)
            }
            case None => { // Insert a new user
              dao.insert(User.create(
                loginInfo = user.loginInfo,
                firstName = user.firstName,
                lastName = user.lastName,
                fullName = user.fullName,
                email = user.email,
                avatarURL = user.avatarURL,
                activeOrganisation = user.activeOrganisation))
            }
          }
        }
      }
      case None => {
        dao.insert(User.create(
          loginInfo = user.loginInfo,
          firstName = user.firstName,
          lastName = user.lastName,
          fullName = user.fullName,
          email = user.email,
          avatarURL = user.avatarURL,
          activeOrganisation = user.activeOrganisation))
      }
    }
  }
  * 
  */

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  override def save(profile: CommonSocialProfile): Future[Option[User]] = {

    Logger.info("UserServiceImpl save (profile): " + profile)

    retrieve(profile.loginInfo).flatMap { optionUser =>
      optionUser match {
        case Some(user) => { // Update user with profile
          val updatedUser = user.copy(
            loginInfo = profile.loginInfo,
            firstName = profile.firstName.getOrElse(user.firstName),
            lastName = profile.lastName.getOrElse(user.lastName),
            name = profile.fullName.getOrElse(user.name),
            email = profile.email.getOrElse(user.email),
            avatarURL = Some(profile.avatarURL.getOrElse(user.avatarURL.getOrElse(""))))
          update(updatedUser) flatMap {
            _ match {
              case true => findOne(DbModel.queryByUuid(user.uuid))
              case false => Future.successful(None)
            }
          }
        }
        case None => { // Insert a new user
          val newUser = User.create(
            loginInfo = profile.loginInfo,
            firstName = profile.firstName.getOrElse(""),
            lastName = profile.lastName.getOrElse(""),
            name = profile.fullName.getOrElse(""),
            email = profile.email.getOrElse(""),
            avatarURL = profile.avatarURL)
          insert(newUser)
        }
      }
    }
  }
}