package viewdata

import models.Domain
import models.Hierarchy
import models.Element

case class ElementData(domain: Domain, hierarchy: Hierarchy, elementChain: List[Element])