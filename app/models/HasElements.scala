package models

trait HasElements {
  val elements: Set[String]
}

object HasElements {
  private val KeyElements = "elements"
}
