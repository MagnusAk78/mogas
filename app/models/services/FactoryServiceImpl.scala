package models.services

import java.io.PipedInputStream
import java.io.PipedOutputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import javax.inject.Inject
import models.AmlFiles
import models.Factory
import models.ExternalInterface
import models.Hierarchy
import models.InternalElement
import models.daos.FactoryDAO
import reactivemongo.play.json.JsFieldBSONElementProducer
import utils.PaginateData
import play.api.Logger
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import models.FactoryPart
import models.HierarchyPart
import models.NamedModel
import play.api.libs.streams.Streams
import akka.stream.scaladsl.Source
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.iteratee.Iteratee
import models.Images
import java.io.BufferedInputStream

class FactoryServiceImpl @Inject() (
  override val dao: FactoryDAO,
  userService: UserService,
  fileService: FileService,
  hierarchyService: HierarchyService,
  internalElementService: InternalElementService,
  externalInterfaceService: ExternalInterfaceService)(implicit val ec: ExecutionContext)
    extends FactoryService {

  override def getFactoryList(page: Int, organisation: String): Future[ModelListData[Factory]] = {
    for {
      factoryList <- find(Factory.queryByOrganisation(organisation), page, utils.DefaultValues.DefaultPageLength)
      factoryCount <- count(Factory.queryByOrganisation(organisation))
    } yield new ModelListData[Factory] {
      override val list = factoryList
      override val paginateData = PaginateData(page, factoryCount)
    }
  }

  override def remove(factory: Factory, loggedInUserUuid: String): Future[RemoveResult] = {

    //TODO: Is this allowed?
    dao.remove(factory).map(success => if (success) {
      RemoveResult(true, None)
    } else {
      RemoveResult(false, Some("DAO refused to remove factory: " + factory.uuid))
    })
  }

  override def parseAmlFiles(factory: Factory): Future[Boolean] = {

    Logger.info("parseAmlFiles, factory: " + factory)

    for {
      fileList <- fileService.findByQuery(AmlFiles.getQueryAllAmlFiles(factory.uuid)).flatMap(_.collect[List](0, true))
      updateResult <- {
        var hierarchies = List.empty[String]

        fileList.foreach { file =>
          fileService.withSyncGfs { gfs =>

            val fileIterator = fileService.withAsyncGfs[Array[Byte]] { gfs =>
              gfs.enumerate(file).
                run(Iteratee.consume[Array[Byte]]())
            }

            val futureAmlHandledResult = fileIterator.flatMap {
              bytes =>
                {
                  val inputStream = new BufferedInputStream(new ByteArrayInputStream(bytes))
                  updateAmlHierarchies(factory, AmlHelper.generateFromStream(inputStream))
                }
            }

            hierarchies = hierarchies ::: Await.result(futureAmlHandledResult, Duration("20s"))
          }
        }

        update(factory.copy(factoryHierachies = hierarchies.toSet))
      }
    } yield updateResult
  }

  private def updateAmlHierarchies(factory: Factory, amlHierarchies: List[AmlHierarchy]): Future[List[String]] = {

    Logger.info("updateAmlHierarchies, amlHierarchies: " + amlHierarchies.map(_.name))

    Future.sequence(
      amlHierarchies.map(amlHierarchy => updateAmlHierarchy(factory, amlHierarchy)))
  }

  private def updateAmlHierarchy(factory: Factory, amlHierarchy: AmlHierarchy): Future[String] = {
    for {
      optionalOldHierarchy <- hierarchyService.findOne(FactoryPart.
        queryByFactory(factory) ++ NamedModel.queryByName(amlHierarchy.name))
      existingHierarchy <- optionalOldHierarchy match {
        case Some(h) => Future.successful(h)
        case None => hierarchyService.insert(Hierarchy.
          create(factory = factory.uuid, name = amlHierarchy.name, orderNumber = amlHierarchy.orderNumber)).
          map(h => h.get)
      }
      internalElements <- updateInternalElements(factory, existingHierarchy.uuid, true,
        amlHierarchy.internalElements)
      updateResult <- {
        //This was an existing hierarchy, update it in the db
        val updatedHierarchy = existingHierarchy.copy(orderNumber = amlHierarchy.orderNumber,
          internalElements = internalElements.toSet)
        hierarchyService.update(updatedHierarchy).map(s => updatedHierarchy.uuid)
      }
    } yield updateResult
  }

  private def updateInternalElements(factory: Factory, parent: String, parentIsHierarchy: Boolean,
                                     amlInternalElements: List[AmlInternalElement]): Future[List[String]] = {

    Future.sequence(
      amlInternalElements.map(elements => updateInternalElement(factory, parent, parentIsHierarchy, elements)))
  }

  private def updateInternalElement(factory: Factory, parent: String, parentIsHierarchy: Boolean,
                                    amlInternalElement: AmlInternalElement): Future[String] = {

    for {
      optionalOldIE <- {
        val query = HierarchyPart.queryByAmlId(amlInternalElement.amlId) ++ FactoryPart.queryByFactory(factory)
        internalElementService.findOne(query)
      }

      existingIE <- {
        optionalOldIE match {
          case Some(ie) => Future.successful(ie)
          case None => internalElementService.insert(InternalElement.
            create(factory.uuid, amlInternalElement.name, parent, parentIsHierarchy, amlInternalElement.orderNumber,
              amlInternalElement.amlId)).
            map(ie => ie.get)
        }
      }
      currentExternalInterfaces <- updateExternalInterfaces(factory, existingIE.uuid,
        amlInternalElement.externalInterfaces)
      currentInternalElements <- updateInternalElements(factory, existingIE.uuid, false,
        amlInternalElement.internalElements)
      updatedOrNewUuid <- {
        val updatedInternalElement = existingIE.copy(
          name = amlInternalElement.name,
          internalElements = currentInternalElements.toSet,
          externalInterfaces = currentExternalInterfaces.toSet,
          orderNumber = amlInternalElement.orderNumber,
          parent = parent)

        internalElementService.update(updatedInternalElement).map(s => updatedInternalElement.uuid)
      }

    } yield updatedOrNewUuid
  }

  private def updateExternalInterfaces(
    factory: Factory,
    parentInternalElement: String,
    amlExternalInterfaces: List[AmlExternalInterface]): Future[List[String]] = {

    Future.sequence(
      amlExternalInterfaces.map(interface => updateExternalInterface(factory, parentInternalElement, interface)))
  }

  private def updateExternalInterface(factory: Factory, parentInternalElement: String,
                                      amlExternalInterface: AmlExternalInterface): Future[String] = {
    for {
      existingExternalInterface <- {
        val query = HierarchyPart.queryByAmlId(amlExternalInterface.amlId) ++ FactoryPart.queryByFactory(factory)
        externalInterfaceService.findOne(query)
      }
      updatedOrNewUuid <- existingExternalInterface match {
        case Some(fei) =>
          //This was an existing external interface, update it in the db
          val updatedExternalInterface = fei.copy(name = amlExternalInterface.name, factory = factory.uuid,
            orderNumber = amlExternalInterface.orderNumber, parent = parentInternalElement)

          externalInterfaceService.update(updatedExternalInterface).map(s => updatedExternalInterface.uuid)
        case None =>
          //This was a new external interface, insert it in the db
          val newExternalInterface = ExternalInterface.create(
            factory = factory.uuid,
            name = amlExternalInterface.name,
            orderNumber = amlExternalInterface.orderNumber,
            amlId = amlExternalInterface.amlId,
            parent = parentInternalElement)

          externalInterfaceService.insert(newExternalInterface).map(r => newExternalInterface.uuid)
      }
    } yield updatedOrNewUuid
  }
}