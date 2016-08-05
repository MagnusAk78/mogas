package utils

import models.Hierarchy
import models.Factory

case class AmlObjectChain(
  val amlObjectChain: List[ElementOrInterface],
  val hierarchy: Hierarchy,
  val factory: Factory) {}

