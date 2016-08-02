package models.daos

import models.Factory

trait FactoryDAO extends BaseModelDAO[Factory] {
  override val companionObject = Factory
}