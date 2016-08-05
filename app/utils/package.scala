import models.Interface
import models.Element

package object utils {
  type ElementOrInterface = Either[Element, Interface]
}
