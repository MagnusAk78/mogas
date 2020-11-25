package models

trait HasModelType {
  val modelType: String
}

object HasModelType {
  private val KeyModelType = "modelType"
}