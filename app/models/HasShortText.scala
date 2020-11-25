package models

trait HasShortText {
  val shortText: String
}

object HasShortText {
  private val KeyShortText = "shortText"
}
