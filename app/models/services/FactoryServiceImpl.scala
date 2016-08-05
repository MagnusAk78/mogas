package models.services

import java.io.PipedInputStream
import utils.ModelListData
import java.io.PipedOutputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import javax.inject.Inject
import models.AmlFiles
import models.Factory
import models.Interface
import models.Hierarchy
import models.Element
import models.daos.FactoryDAO
import models.daos.HierarchyDAO
import reactivemongo.play.json.JsFieldBSONElementProducer
import utils.PaginateData
import play.api.Logger
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import models.ChildOf
import models.AmlObject
import models.NamedModel
import play.api.libs.streams.Streams
import akka.stream.scaladsl.Source
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.iteratee.Iteratee
import models.Images
import java.io.BufferedInputStream
import models.Organisation
import play.api.libs.json.JsObject
import utils.RemoveResult
import models.services.misc.AmlInterface
import models.services.misc.AmlHierarchy
import models.services.misc.AmlHelper
import models.services.misc.AmlElement
import utils.AmlObjectChain
import utils.ElementOrInterface

class FactoryServiceImpl @Inject() (
  val factoryDao: FactoryDAO,
  val hierarchyDao: HierarchyDAO,
  userService: UserService,
  fileService: FileService,
  amlObjectService: AmlObjectService)(implicit val ec: ExecutionContext)
    extends FactoryService {

  override def getFactoryList(page: Int, organisation: Organisation): Future[ModelListData[Factory]] = {
    findManyFactories(Factory.queryByParent(organisation), page, utils.DefaultValues.DefaultPageLength)
  }

  override def removeFactory(factory: Factory, loggedInUserUuid: String): Future[RemoveResult] = {

    //TODO: Is this allowed?
    factoryDao.remove(factory).map(success => if (success) {
      RemoveResult(true, None)
    } else {
      RemoveResult(false, Some("DAO refused to remove factory: " + factory.uuid))
    })
  }

  override def insertFactory(model: Factory): Future[Option[Factory]] = factoryDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateFactory(model: Factory): Future[Boolean] = factoryDao.update(model).map(wr => wr.ok)

  override def findOneFactory(query: JsObject): Future[Option[Factory]] = factoryDao.find(query, 1, 1).map(_.headOption)

  override def findManyFactories(query: JsObject, page: Int = 1,
                                 pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Factory]] = {
    for {
      theList <- factoryDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- factoryDao.count(query)
    } yield new ModelListData[Factory] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
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

        updateFactory(factory.copy(hierachies = hierarchies.toSet))
      }
    } yield updateResult
  }

  override def getHierarchyList(page: Int, factory: Factory): Future[ModelListData[Hierarchy]] = {
    findManyHierarchies(Hierarchy.queryByParent(factory), page, utils.DefaultValues.DefaultPageLength)
  }

  override def insertHierarchy(model: Hierarchy): Future[Option[Hierarchy]] = hierarchyDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateHierarchy(model: Hierarchy): Future[Boolean] = hierarchyDao.update(model).map(wr => wr.ok)

  override def findOneHierarchy(query: JsObject): Future[Option[Hierarchy]] = hierarchyDao.find(query, 1, 1).map(_.headOption)

  override def findManyHierarchies(query: JsObject, page: Int = 1,
                                   pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Hierarchy]] = {
    for {
      theList <- hierarchyDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- hierarchyDao.count(query)
    } yield new ModelListData[Hierarchy] {
      override val list = theList
      override val paginateData = PaginateData(page, count)
    }
  }

  private def updateAmlHierarchies(factory: Factory, amlHierarchies: List[AmlHierarchy]): Future[List[String]] = {

    Logger.info("updateAmlHierarchies, amlHierarchies: " + amlHierarchies.map(_.name))

    Future.sequence(
      amlHierarchies.map(amlHierarchy => updateAmlHierarchy(factory, amlHierarchy)))
  }

  private def updateAmlHierarchy(factory: Factory, amlHierarchy: AmlHierarchy): Future[String] = {
    for {
      optionalOldHierarchy <- findOneHierarchy(Hierarchy.queryByParent(factory) ++
        Hierarchy.queryByName(amlHierarchy.name))
      existingHierarchy <- optionalOldHierarchy match {
        case Some(h) => Future.successful(h)
        case None => insertHierarchy(Hierarchy.
          create(parentFactory = factory.uuid, name = amlHierarchy.name, orderNumber = amlHierarchy.orderNumber)).
          map(h => h.get)
      }
      elements <- updateElements(factory, existingHierarchy.uuid, true,
        amlHierarchy.elements)
      updateResult <- {
        //This was an existing hierarchy, update it in the db
        val updatedHierarchy = existingHierarchy.copy(orderNumber = amlHierarchy.orderNumber,
          elements = elements.toSet)
        updateHierarchy(updatedHierarchy).map(s => updatedHierarchy.uuid)
      }
    } yield updateResult
  }

  private def updateElements(factory: Factory, parent: String, parentIsHierarchy: Boolean,
                             amlElements: List[AmlElement]): Future[List[String]] = {

    Future.sequence(
      amlElements.map(elements => updateElement(factory, parent, parentIsHierarchy, elements)))
  }

  private def updateElement(factory: Factory, parent: String, parentIsHierarchy: Boolean,
                            amlElement: AmlElement): Future[String] = {

    for {
      optionalOldIE <- {
        val query = Element.queryByAmlId(amlElement.amlId) ++ Element.queryByConnectionTo(factory)
        amlObjectService.findOneElement(query)
      }

      existingIE <- {
        optionalOldIE match {
          case Some(ie) => Future.successful(ie)
          case None => amlObjectService.insertElement(Element.
            create(factory.uuid, amlElement.name, parent, parentIsHierarchy, amlElement.orderNumber,
              amlElement.amlId)).
            map(ie => ie.get)
        }
      }
      currentInterfaces <- updateInterfaces(factory, existingIE.uuid,
        amlElement.interfaces)
      currentElements <- updateElements(factory, existingIE.uuid, false,
        amlElement.elements)
      updatedOrNewUuid <- {
        val updatedElement = existingIE.copy(
          name = amlElement.name,
          elements = currentElements.toSet,
          interfaces = currentInterfaces.toSet,
          orderNumber = amlElement.orderNumber,
          parent = parent)

        amlObjectService.updateElement(updatedElement).map(s => updatedElement.uuid)
      }

    } yield updatedOrNewUuid
  }

  private def updateInterfaces(
    factory: Factory,
    parentElement: String,
    amlInterfaces: List[AmlInterface]): Future[List[String]] = {

    Future.sequence(
      amlInterfaces.map(interface => updateInterface(factory, parentElement, interface)))
  }

  private def updateInterface(factory: Factory, parentElement: String,
                              amlInterface: AmlInterface): Future[String] = {
    for {
      existingInterface <- {
        val query = Interface.queryByAmlId(amlInterface.amlId) ++ Interface.queryByConnectionTo(factory)
        amlObjectService.findOneInterface(query)
      }
      updatedOrNewUuid <- existingInterface match {
        case Some(fei) =>
          //This was an existing external interface, update it in the db
          val updatedInterface = fei.copy(name = amlInterface.name, connectionTo = factory.uuid,
            orderNumber = amlInterface.orderNumber, parent = parentElement)

          amlObjectService.updateInterface(updatedInterface).map(s => updatedInterface.uuid)
        case None =>
          //This was a new external interface, insert it in the db
          val newInterface = Interface.create(
            connectionToFactory = factory.uuid,
            name = amlInterface.name,
            orderNumber = amlInterface.orderNumber,
            amlId = amlInterface.amlId,
            parent = parentElement)

          amlObjectService.insertInterface(newInterface).map(r => newInterface.uuid)
      }
    } yield updatedOrNewUuid
  }

  override def getAmlObjectChains(children: List[ChildOf[AmlObject]]): Future[List[AmlObjectChain]] =
    Future.successful(children.map(child => getAmlObjectChain(child.parent)))

  private def getAmlObjectChain(uuid: String): AmlObjectChain = {
    val chain = genereateAmlObjectChain(uuid, List())
    val hierarchy = Await.result(findOneHierarchy(Hierarchy.queryByUuid(chain.head.fold(_.parent, _.parent))),
      Duration("3s")).get
    val factory = Await.result(findOneFactory(Factory.queryByUuid(hierarchy.parent)), Duration("3s")).get

    AmlObjectChain(chain, hierarchy, factory)
  }

  private def genereateAmlObjectChain(uuid: String, list: List[ElementOrInterface]): List[ElementOrInterface] = {
    val optElement = Await.result(amlObjectService.findOneElement(Element.queryByUuid(uuid)), Duration("3s"))
    Logger.info("optElement: " + optElement)
    val optInterface = optElement.map(element => None).
      getOrElse(Await.result(amlObjectService.findOneInterface(Interface.queryByUuid(uuid)),
        Duration("3s")))

    optElement match {
      case Some(element) => genereateAmlObjectChain(element.parent, Left(element) :: list)
      case None => optInterface match {
        case Some(interface) => genereateAmlObjectChain(interface.parent, Right(interface) :: list)
        case None => list
      }
    }
  }
}