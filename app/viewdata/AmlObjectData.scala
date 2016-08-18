package viewdata

import models._

case class AmlObjectData(
    domain: Domain, 
    hierarchy: Hierarchy, 
    amlObjectChain: List[DbModel with HasName with HasModelType])

