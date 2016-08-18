package viewdata

import viewdata.NavTypes
import models.User
import models.Domain

case class MainViewData(title: String, navType: NavTypes.NavType, userStatus: UserStatus)