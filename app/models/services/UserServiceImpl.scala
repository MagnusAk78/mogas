package models.services

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import viewdata.ModelListData
import javax.inject.Inject
import models.User
import models.daos.UserDAO
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import viewdata.PaginateData

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (val dao: UserDAO, val fileService: FileService)(implicit val ec: ExecutionContext)
    extends UserService {

  implicit val joWrites: OWrites[User] = User.userFormat

  implicit val joReads: Reads[User] = User.userFormat

  override def getUserList(page: Int, uuidSet: Set[String]): Future[ModelListData[User]] = {
    findMany(User.queryBySetOfUuids(uuidSet), page, utils.DefaultValues.DefaultPageLength)
  }

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = findOne(User.queryByLoginInfo(loginInfo))

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
                activeDomain = user.activeDomain)
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
                activeDomain = user.activeDomain))
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
          activeDomain = user.activeDomain))
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
              case true => findOne(User.queryByUuid(user.uuid))
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

  override def insert(user: User): Future[Option[User]] = dao.insert(user).map(wr => if (wr.ok) Some(user) else None)

  override def update(user: User): Future[Boolean] = dao.update(user).map(wr => wr.ok)

  override def findOne(query: JsObject): Future[Option[User]] = dao.find(query, 1, 1).map(_.headOption)

  override def findMany(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[User]] = {
    for {
      theList <- dao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- dao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[User] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }
}