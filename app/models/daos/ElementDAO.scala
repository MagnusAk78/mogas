package models.daos

import models.Element

trait ElementDAO extends BaseModelDAO[Element] {
  override val companionObject = Element
}