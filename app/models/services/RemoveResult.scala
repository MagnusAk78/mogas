package models.services

case class RemoveResult(
    val success: Boolean,
    val reason: Option[String] = None
) {
  
}