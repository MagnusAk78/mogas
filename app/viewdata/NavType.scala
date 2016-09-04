package viewdata

object NavTypes extends Enumeration {

  sealed trait NavType {
    val stringValue: String
  }

  final case object Domains extends NavType {
    override val stringValue = "Domains"
  }

  final case object Users extends NavType {
    override val stringValue = "Users"
  }

  final case object Browse extends NavType {
    override val stringValue = "Browse"
  }

  final case object None extends NavType {
    override val stringValue = "None"
  }

  def fromString(navType: String): NavType = navType match {
    case Domains.stringValue => Domains
    case Users.stringValue => Users
    case None.stringValue => None
  }
}