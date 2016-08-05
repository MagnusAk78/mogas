package models.daos

import models.IssueUpdate

trait IssueUpdateDAO extends BaseModelDAO[IssueUpdate] {
  override val companionObject = IssueUpdate
}