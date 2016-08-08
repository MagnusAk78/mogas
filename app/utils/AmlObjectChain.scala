package utils

import models.Hierarchy
import models.Domain

case class AmlObjectChain(
  val amlObjectChain: List[ElementOrInterface],
  val hierarchy: Hierarchy,
  val domain: Domain) {}

