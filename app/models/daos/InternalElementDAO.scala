package models.daos

import models.InternalElement

trait InternalElementDAO extends BaseModelDAO[InternalElement] {
  override val companionObject = InternalElement
}