package models

object Types {
  sealed trait ModelType {
    val stringValue: String
  }

  final case object OrganisationType extends ModelType {
    override val stringValue = "Organisation"
  }

  final case object UserType extends ModelType {
    override val stringValue = "User"
  }

  final case object FactoryType extends ModelType {
    override val stringValue = "Factory"
  }

  final case object HierarchyType extends ModelType {
    override val stringValue = "Hierarchy"
  }

  final case object InternalElementType extends ModelType {
    override val stringValue = "InternalElement"
  }

  final case object ExternalInterfaceType extends ModelType {
    override val stringValue = "ExternalInterface"
  }

  final case object UnknownType extends ModelType {
    override val stringValue = "UnknownType"
  }

  def fromString(modelType: String): ModelType = {
    modelType match {
      case OrganisationType.stringValue => OrganisationType
      case UserType.stringValue => UserType
      case FactoryType.stringValue => FactoryType
      case HierarchyType.stringValue => HierarchyType
      case InternalElementType.stringValue => InternalElementType
      case ExternalInterfaceType.stringValue => ExternalInterfaceType
      case _ => UnknownType
    }
  }
}