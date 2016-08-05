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

  final case object ElementType extends ModelType {
    override val stringValue = "Element"
  }

  final case object InterfaceType extends ModelType {
    override val stringValue = "Interface"
  }

  final case object InstructionType extends ModelType {
    override val stringValue = "Instruction"
  }

  final case object InstructionPartType extends ModelType {
    override val stringValue = "InstructionPart"
  }

  final case object IssueType extends ModelType {
    override val stringValue = "Issue"
  }

  final case object IssueUpdateType extends ModelType {
    override val stringValue = "IssueUpdate"
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
      case ElementType.stringValue => ElementType
      case InterfaceType.stringValue => InterfaceType
      case InstructionType.stringValue => InstructionType
      case InstructionPartType.stringValue => InstructionPartType
      case IssueType.stringValue => IssueType
      case IssueUpdateType.stringValue => IssueUpdateType
      case _ => UnknownType
    }
  }
}