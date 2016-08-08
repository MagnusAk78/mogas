package utils

object NavTypes extends Enumeration {

  sealed trait NavType {
    val stringValue: String
  }

  final case object Organisations extends NavType {
    override val stringValue = "Organisations"
  }

  final case object Users extends NavType {
    override val stringValue = "Users"
  }

  final case object Factories extends NavType {
    override val stringValue = "Factories"
  }

  final case object Instructions extends NavType {
    override val stringValue = "Instructions"
  }

  final case object Issues extends NavType {
    override val stringValue = "Issues"
  }

  final case object None extends NavType {
    override val stringValue = "None"
  }

  def fromString(navType: String): NavType = navType match {
    case Organisations.stringValue => Organisations
    case Users.stringValue => Users
    case Factories.stringValue => Factories
    case Instructions.stringValue => Instructions
    case Issues.stringValue => Issues
    case None.stringValue => None
  }
}