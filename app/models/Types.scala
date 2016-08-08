package models

import utils.NavTypes._
import utils._

object Types {
  sealed trait ModelType {
    val stringValue: String
    val navType: NavType
  }

  final case object UserType extends ModelType {
    override val stringValue = "User"
    override val navType = NavTypes.Users
  }

  final case object DomainType extends ModelType {
    override val stringValue = "Domain"
    override val navType = NavTypes.Domains
  }

  final case object HierarchyType extends ModelType {
    override val stringValue = "Hierarchy"
    override val navType = NavTypes.Browse
  }

  final case object ElementType extends ModelType {
    override val stringValue = "Element"
    override val navType = NavTypes.Browse
  }

  final case object InterfaceType extends ModelType {
    override val stringValue = "Interface"
    override val navType = NavTypes.Browse
  }

  final case object InstructionType extends ModelType {
    override val stringValue = "Instruction"
    override val navType = NavTypes.Instructions
  }

  final case object InstructionPartType extends ModelType {
    override val stringValue = "InstructionPart"
    override val navType = NavTypes.Instructions
  }

  final case object IssueType extends ModelType {
    override val stringValue = "Issue"
    override val navType = NavTypes.Issues
  }

  final case object IssueUpdateType extends ModelType {
    override val stringValue = "IssueUpdate"
    override val navType = NavTypes.Issues
  }

  final case object UnknownType extends ModelType {
    override val stringValue = "UnknownType"
    override val navType = NavTypes.None
  }

  def fromString(modelType: String): ModelType = {
    modelType match {
      case DomainType.stringValue => DomainType
      case UserType.stringValue => UserType
      case DomainType.stringValue => DomainType
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