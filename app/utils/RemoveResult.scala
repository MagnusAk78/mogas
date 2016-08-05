package utils

case class RemoveResult(
    val success: Boolean,
    val reason: Option[String] = None
) {
  def getReason: String = reason.map { r => r }.getOrElse(RemoveResult.NO_REASON)
}

object RemoveResult {
  val NO_REASON = "NO-REASON"
}