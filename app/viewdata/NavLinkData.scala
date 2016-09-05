package viewdata

import play.api.mvc.Call

case class NavLinkData(link: Call, label: String, active: Boolean)