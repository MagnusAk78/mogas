package models.daos

import models.Issue

trait IssueDAO extends BaseModelDAO[Issue] {
  override val companionObject = Issue
}