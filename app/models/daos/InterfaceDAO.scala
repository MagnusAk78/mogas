package models.daos

import models.Interface

trait InterfaceDAO extends BaseModelDAO[Interface] {
  override val companionObject = Interface
}