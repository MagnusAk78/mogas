package models.daos

import models.Hierarchy

trait HierarchyDAO extends BaseModelDAO[Hierarchy] {
  override val companionObject = Hierarchy
}