package models.services

import utils.PaginateData
import models.Organisation
import models.daos.InternalElementDAO
import models.Hierarchy
import models.Factory
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json
import models.InternalElement
import models.HierarchyPart
import models.AmlParent
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.Logger

class InternalElementServiceImpl @Inject() (override val dao: InternalElementDAO,
                                            externalInterfaceService: ExternalInterfaceService)(implicit val ec: ExecutionContext) extends InternalElementService {

  override def getInternalElementList(page: Int, parent: AmlParent): Future[ModelListData[InternalElement]] = {
    for {
      internalElementList <- find(HierarchyPart.queryByParent(parent), page, utils.DefaultValues.DefaultPageLength)
      internalElementCount <- count(HierarchyPart.queryByParent(parent))
    } yield new ModelListData[InternalElement] {
      override val list = internalElementList
      override val paginateData = PaginateData(page, internalElementCount)
    }
  }

  override def getElementChain(uuid: String): Future[List[InternalElement]] = {

    Logger.info("InternalElementService getElementChain from uuid: " + uuid)

    for {
      optionElement <- findOneByUuid(uuid)
    } yield optionElement match {
      case Some(element) => {
        if (element.parentIsHierarchy) {
          Logger.info("InternalElementService getElementChain element: " + element)
          List(element)
        } else {
          Logger.info("InternalElementService getElementChain recursive with element: " + element)
          Await.result(getElementChain(element.parent), Duration("5s")) ++ List(element)
        }
      }
      case None => List()
    }
  }
}