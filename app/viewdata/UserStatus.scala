package viewdata

import models.User
import models.Domain

case class UserStatus(loggedInUser: Option[User], activeDomain: Option[Domain])