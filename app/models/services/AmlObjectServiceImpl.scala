package models.services

import scala.Left
import scala.Right
import scala.annotation.implicitNotFound
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import javax.inject.Inject
import models.Element
import models.Domain
import models.Hierarchy
import models.Instruction
import models.Interface
import models.daos.InterfaceDAO
import play.api.Logger
import play.api.libs.json.JsObject
import views.html.instructions.instruction
import models.daos.ElementDAO
import viewdata.PaginateData
import viewdata.ModelListData
import models.HasElements
import models.HasAmlId
import models.DbModel

class AmlObjectServiceImpl @Inject() (
    val elementDao: ElementDAO,
    val interfaceDao: InterfaceDAO,
    val fileService: FileService)(implicit ec: ExecutionContext) extends AmlObjectService {


  override def getElementList(page: Int, parent: DbModel with HasElements): Future[ModelListData[Element]] =
    findManyElements(Element.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)

  override def getElementChain(uuid: String): Future[List[Element]] = {
    for {
      optionElement <- findOneElement(Element.queryByUuid(uuid))
    } yield optionElement match {
      case Some(element) => {
        if (element.parentIsHierarchy) {
          List(element)
        } else {
          Await.result(getElementChain(element.parent), Duration("5s")) ++ List(element)
        }
      }
      case None => List()
    }
  }

  override def insertElement(model: Element): Future[Option[Element]] =
    elementDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateElement(model: Element): Future[Boolean] = elementDao.update(model).map(wr => wr.ok)

  override def findOneElement(query: JsObject): Future[Option[Element]] =
    elementDao.find(query, 1, 1).map(_.headOption)

  override def findManyElements(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Element]] = {
    for {
      theList <- elementDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- elementDao.count(query)
      il <- Future.sequence(theList.map(e => fileService.imageExists(e.uuid)))
      vl <- Future.sequence(theList.map(e => fileService.videoExists(e.uuid)))
    } yield new ModelListData[Element] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  override def getInterfaceList(page: Int, parent: Element): Future[ModelListData[Interface]] = {
    findManyInterfaces(Interface.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)
  }

  override def insertInterface(model: Interface): Future[Option[Interface]] =
    interfaceDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateInterface(model: Interface): Future[Boolean] =
    interfaceDao.update(model).map(wr => wr.ok)

  override def findOneInterface(query: JsObject): Future[Option[Interface]] =
    interfaceDao.find(query, 1, 1).map(_.headOption)

  override def findManyInterfaces(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Interface]] = {
    for {
      theList <- interfaceDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- interfaceDao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[Interface] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }
}
