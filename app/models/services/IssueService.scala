package models.services

import scala.concurrent.Future
import models.Issue
import utils.PaginateData
import models.Factory
import models.IssueUpdate
import play.api.libs.json.JsObject
import utils.RemoveResult
import utils.ModelListData

trait IssueService {

  def getIssueList(page: Int, factory: Option[Factory] = None): Future[ModelListData[Issue]]

  def insertIssue(model: Issue): Future[Option[Issue]]

  def updateIssue(model: Issue): Future[Boolean]

  def findOneIssue(query: JsObject): Future[Option[Issue]]

  def findManyIssues(query: JsObject, page: Int = 1,
                     pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Issue]]

  def getIssueUpdateList(issue: Issue, page: Int): Future[ModelListData[IssueUpdate]]

  def getNextOrderNumber(issue: Issue): Future[Int]

  def insertIssueUpdate(model: IssueUpdate): Future[Option[IssueUpdate]]

  def updateIssueUpdate(model: IssueUpdate): Future[Boolean]

  def findOneIssueUpdate(query: JsObject): Future[Option[IssueUpdate]]

  def findManyIssueUpdates(query: JsObject, page: Int = 1,
                           pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]]

  def findManySortedIssueUpdates(query: JsObject, sort: JsObject, page: Int = 1,
                                 pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]]
}