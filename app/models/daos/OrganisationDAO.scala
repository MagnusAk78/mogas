package models.daos

import models.Organisation

trait OrganisationDAO extends BaseModelDAO[Organisation] {
  override val companionObject = Organisation
}