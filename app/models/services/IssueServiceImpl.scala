package models.services

import javax.inject.Inject
import models.daos.{IssueDAO, IssueUpdateDAO}
import models._
import play.api.libs.json.JsObject
import viewdata.{ModelListData, PaginateData}

import scala.concurrent.{ExecutionContext, Future}

class IssueServiceImpl @Inject() (val issueDao: IssueDAO,
  val issueUpdateDao: IssueUpdateDAO,
  val amlObjectService: AmlObjectService,
  val fileService: FileService)(implicit val ec: ExecutionContext)
    extends IssueService {

  override def getIssueList(page: Int, domain: Option[Domain] = None): Future[ModelListData[Issue]] = {
    val selector = domain.map(f => HasConnectionTo.queryByHasConnectionTo(f.uuid)).getOrElse(DbModel.queryAll)
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
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[Issue] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  override def getIssueUpdateList(issue: Issue, page: Int): Future[ModelListData[IssueUpdate]] = {
    findManySortedIssueUpdates(HasParent.queryByParent(issue.uuid), OrderedModel.sortByOrderNumber,
      page, utils.DefaultValues.DefaultPageLength)
  }

  def getNextOrderNumber(issue: Issue): Future[Int] = {
    issueUpdateDao.count(HasParent.queryByParent(issue.uuid)).map { count => count + 1 }
  }

  override def insertIssueUpdate(model: IssueUpdate): Future[Option[IssueUpdate]] = 
    issueUpdateDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateIssueUpdate(model: IssueUpdate): Future[Boolean] = issueUpdateDao.update(model).map(wr => wr.ok)

  override def findOneIssueUpdate(query: JsObject): Future[Option[IssueUpdate]] = issueUpdateDao.find(query, 1, 1).map(_.headOption)

  override def findManyIssueUpdates(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]] = {
    for {
      theList <- issueUpdateDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- issueUpdateDao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[IssueUpdate] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  override def findManySortedIssueUpdates(query: JsObject, sort: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[IssueUpdate]] = {
    for {
      theList <- issueUpdateDao.findAndSort(query, sort, page, utils.DefaultValues.DefaultPageLength)
      count <- issueUpdateDao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[IssueUpdate] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }
}