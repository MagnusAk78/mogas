package models.services

import utils.PaginateData
import models.Issue
import models.daos.IssueDAO
import models.daos.IssueUpdateDAO
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json
import models.Factory
import models.IssueUpdate
import views.html.factories.factory
import models.AmlObject
import utils.AmlObjectChain
import play.api.libs.json.JsObject
import utils.RemoveResult
import utils.ModelListData

class IssueServiceImpl @Inject() (val issueDao: IssueDAO,
                                  val issueUpdateDao: IssueUpdateDAO,
                                  val amlObjectService: AmlObjectService)(implicit val ec: ExecutionContext)
    extends IssueService {

  override def getIssueList(page: Int, factory: Option[Factory] = None): Future[ModelListData[Issue]] = {
    val selector = factory.map(f => Issue.queryByConnectionTo(f)).getOrElse(Issue.queryAll)
    findManyIssues(selector, page, utils.DefaultValues.DefaultPageLength)
  }

  override def insertIssue(model: Issue): Future[Option[Issue]] = issueDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateIssue(model: Issue): Future[Boolean] = issueDao.update(model).map(wr => wr.ok)

  override def findOneIssue(query: JsObject): Future[Option[Issue]] = issueDao.find(query, 1, 1).map(_.headOption)

  override def findManyIssues(query: JsObject, page: Int = 1,
                              pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Issue]] = {
    for {
      theList <- issueDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- issueDao.count(query)
    } yield new ModelListData[Issue] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }

  override def getIssueUpdateList(issue: Issue, page: Int): Future[ModelListData[IssueUpdate]] = {
    findManySortedIssueUpdates(IssueUpdate.queryByParent(issue), IssueUpdate.sortByOrderNumber,
      page, utils.DefaultValues.DefaultPageLength)
  }

  def getNextOrderNumber(issue: Issue): Future[Int] = {
    issueUpdateDao.count(IssueUpdate.queryByParent(issue)).map { count => count + 1 }
  }

  override def insertIssueUpdate(model: IssueUpdate): Future[Option[IssueUpdate]] = issueUpdateDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateIssueUpdate(model: IssueUpdate): Future[Boolean] = issueUpdateDao.update(model).map(wr => wr.ok)

  override def findOneIssueUpdate(query: JsObject): Future[Option[IssueUpdate]] = issueUpdateDao.find(query, 1, 1).map(_.headOption)

  override def findManyIssueUpdates(query: JsObject, page: Int = 1,
                                    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]] = {
    for {
      theList <- issueUpdateDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- issueUpdateDao.count(query)
    } yield new ModelListData[IssueUpdate] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }

  override def findManySortedIssueUpdates(query: JsObject, sort: JsObject, page: Int = 1,
                                          pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]] = {
    for {
      theList <- issueUpdateDao.findAndSort(query, sort, page, utils.DefaultValues.DefaultPageLength)
      count <- issueUpdateDao.count(query)
    } yield new ModelListData[IssueUpdate] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }
}