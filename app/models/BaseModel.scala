package models


trait BaseModel {
  val uuid: Option[String]
}

abstract class BaseModelCompanion {
  
  val KeyUUID = "UUID"
}
