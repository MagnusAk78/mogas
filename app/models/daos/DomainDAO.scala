package models.daos

import models.Domain

trait DomainDAO extends BaseModelDAO[Domain] {
  override val companionObject = Domain
}