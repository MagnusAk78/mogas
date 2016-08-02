package models.daos

import models.ExternalInterface

trait ExternalInterfaceDAO extends BaseModelDAO[ExternalInterface] {
  override val companionObject = ExternalInterface
}