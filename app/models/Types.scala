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
  
  final case object UnknownType extends ModelType {
    override val stringValue = "UnknownType"
  }
  
  def fromString(modelType: String): ModelType = {
    modelType match {
      case OrganisationType.stringValue => OrganisationType
      case UserType.stringValue => UserType
      case _ => UnknownType
    }
  }
}