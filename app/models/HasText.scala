package models

trait HasText {
  val text: String
}

object HasText {
  private val KeyText = "text"
}