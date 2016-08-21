package models.services

import java.io.PipedInputStream
import viewdata.ModelListData
import java.io.PipedOutputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import javax.inject.Inject
import models.AmlFiles
import models.Domain
import models.Interface
import models.Hierarchy
import models.Element
import models.daos.DomainDAO
import models.daos.HierarchyDAO
import reactivemongo.play.json.JsFieldBSONElementProducer
import viewdata.PaginateData
import play.api.Logger
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import models.HasParent
import models.HasAmlId
import models.HasName
import play.api.libs.streams.Streams
import akka.stream.scaladsl.Source
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.iteratee.Iteratee
import models.Images
import java.io.BufferedInputStream
import play.api.libs.json.JsObject
import utils.RemoveResult
import models.services.misc.AmlInterface
import models.services.misc.AmlHierarchy
import models.services.misc.AmlHelper
import models.services.misc.AmlElement
import viewdata.AmlObjectData
import models.HasModelType
import models.User
import models.DbModel

class DomainServiceImpl @Inject() (
  val domainDao: DomainDAO,
  val hierarchyDao: HierarchyDAO,
  userService: UserService,
  fileService: FileService,
  amlObjectService: AmlObjectService)(implicit val ec: ExecutionContext)
    extends DomainService {

  override def getDomainList(page: Int, allowedUser: User): Future[ModelListData[Domain]] = {
    findManyDomains(Domain.queryByAllowedUser(allowedUser), page, utils.DefaultValues.DefaultPageLength)
  }

  override def removeDomain(domain: Domain, loggedInUserUuid: String): Future[RemoveResult] = {

    //TODO: Is this allowed?
    domainDao.remove(domain).map(success => if (success) {
      RemoveResult(true, None)
    } else {
      RemoveResult(false, Some("DAO refused to remove domain: " + domain.uuid))
    })
  }

  override def insertDomain(model: Domain): Future[Option[Domain]] = domainDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateDomain(model: Domain): Future[Boolean] = domainDao.update(model).map(wr => wr.ok)

  override def findOneDomain(query: JsObject): Future[Option[Domain]] = domainDao.find(query, 1, 1).map(_.headOption)

  override def findManyDomains(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Domain]] = {
    for {
      theList <- domainDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- domainDao.count(query)
      il <- Future.sequence(theList.map(d => fileService.imageExists(d.uuid)))
      vl <- Future.sequence(theList.map(d => fileService.videoExists(d.uuid)))
    } yield new ModelListData[Domain] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  override def parseAmlFiles(domain: Domain): Future[Boolean] = {

    Logger.info("parseAmlFiles, domain: " + domain)

    for {
      fileList <- fileService.findByQuery(AmlFiles.getQueryAllAmlFiles(domain.uuid)).flatMap(_.collect[List](0, true))
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
                  updateAmlHierarchies(domain, AmlHelper.generateFromStream(inputStream))
                }
            }

            hierarchies = hierarchies ::: Await.result(futureAmlHandledResult, Duration("20s"))
          }
        }

        updateDomain(domain.copy(hierachies = hierarchies.toSet))
      }
    } yield updateResult
  }

  override def getHierarchyList(page: Int, domain: Domain): Future[ModelListData[Hierarchy]] = {
    findManyHierarchies(Hierarchy.queryByParent(domain), page, utils.DefaultValues.DefaultPageLength)
  }

  override def insertHierarchy(model: Hierarchy): Future[Option[Hierarchy]] = hierarchyDao.insert(model).map(wr => if (wr.ok) Some(model) else None)

  override def updateHierarchy(model: Hierarchy): Future[Boolean] = hierarchyDao.update(model).map(wr => wr.ok)

  override def findOneHierarchy(query: JsObject): Future[Option[Hierarchy]] = hierarchyDao.find(query, 1, 1).map(_.headOption)

  override def findManyHierarchies(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Hierarchy]] = {
    for {
      theList <- hierarchyDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- hierarchyDao.count(query)
      il <- Future.sequence(theList.map(h => fileService.imageExists(h.uuid)))
      vl <- Future.sequence(theList.map(h => fileService.videoExists(h.uuid)))
    } yield new ModelListData[Hierarchy] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  private def updateAmlHierarchies(domain: Domain, amlHierarchies: List[AmlHierarchy]): Future[List[String]] = {

    Logger.info("updateAmlHierarchies, amlHierarchies: " + amlHierarchies.map(_.name))

    Future.sequence(
      amlHierarchies.map(amlHierarchy => updateAmlHierarchy(domain, amlHierarchy)))
  }

  private def updateAmlHierarchy(domain: Domain, amlHierarchy: AmlHierarchy): Future[String] = {
    for {
      optionalOldHierarchy <- findOneHierarchy(Hierarchy.queryByParent(domain) ++
        Hierarchy.queryByName(amlHierarchy.name))
      existingHierarchy <- optionalOldHierarchy match {
        case Some(h) => Future.successful(h)
        case None => insertHierarchy(Hierarchy.
          create(parentDomain = domain.uuid, name = amlHierarchy.name, orderNumber = amlHierarchy.orderNumber)).
          map(h => h.get)
      }
      elements <- updateElements(domain, existingHierarchy.uuid, true,
        amlHierarchy.elements)
      updateResult <- {
        //This was an existing hierarchy, update it in the db
        val updatedHierarchy = existingHierarchy.copy(orderNumber = amlHierarchy.orderNumber,
          elements = elements.toSet)
        updateHierarchy(updatedHierarchy).map(s => updatedHierarchy.uuid)
      }
    } yield updateResult
  }

  private def updateElements(domain: Domain, parent: String, parentIsHierarchy: Boolean,
    amlElements: List[AmlElement]): Future[List[String]] = {

    Future.sequence(
      amlElements.map(elements => updateElement(domain, parent, parentIsHierarchy, elements)))
  }

  private def updateElement(domain: Domain, parent: String, parentIsHierarchy: Boolean,
    amlElement: AmlElement): Future[String] = {

    for {
      optionalOldIE <- {
        val query = Element.queryByAmlId(amlElement.amlId) ++ Element.queryByHasConnectionTo(domain)
        amlObjectService.findOneElement(query)
      }

      existingIE <- {
        optionalOldIE match {
          case Some(ie) => Future.successful(ie)
          case None => amlObjectService.insertElement(Element.
            create(domain.uuid, amlElement.name, parent, parentIsHierarchy, amlElement.orderNumber,
              amlElement.amlId)).
            map(ie => ie.get)
        }
      }
      currentInterfaces <- updateInterfaces(domain, existingIE.uuid,
        amlElement.interfaces)
      currentElements <- updateElements(domain, existingIE.uuid, false,
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
    domain: Domain,
    parentElement: String,
    amlInterfaces: List[AmlInterface]): Future[List[String]] = {

    Future.sequence(
      amlInterfaces.map(interface => updateInterface(domain, parentElement, interface)))
  }

  private def updateInterface(domain: Domain, parentElement: String,
    amlInterface: AmlInterface): Future[String] = {
    for {
      existingInterface <- {
        val query = Interface.queryByAmlId(amlInterface.amlId) ++ Interface.queryByHasConnectionTo(domain)
        amlObjectService.findOneInterface(query)
      }
      updatedOrNewUuid <- existingInterface match {
        case Some(fei) =>
          //This was an existing external interface, update it in the db
          val updatedInterface = fei.copy(name = amlInterface.name, connectionTo = domain.uuid,
            orderNumber = amlInterface.orderNumber, parent = parentElement)

          amlObjectService.updateInterface(updatedInterface).map(s => updatedInterface.uuid)
        case None =>
          //This was a new external interface, insert it in the db
          val newInterface = Interface.create(
            connectionToDomain = domain.uuid,
            name = amlInterface.name,
            orderNumber = amlInterface.orderNumber,
            amlId = amlInterface.amlId,
            parent = parentElement)

          amlObjectService.insertInterface(newInterface).map(r => newInterface.uuid)
      }
    } yield updatedOrNewUuid
  }

  override def getAmlObjectDatas(children: List[DbModel with HasParent]): Future[List[AmlObjectData]] =
    Future.successful(children.map(child => getAmlObjectData(child.parent)))

  private def getAmlObjectData(uuid: String): AmlObjectData = {
    val chain = genereateAmlObjectData(uuid, List())
    val hierarchy = Await.result(findOneHierarchy(Hierarchy.queryByUuid(chain.head.parent)),
      Duration("3s")).get
    val domain = Await.result(findOneDomain(Domain.queryByUuid(hierarchy.parent)), Duration("3s")).get

    AmlObjectData(domain, hierarchy, chain)
  }

  private def genereateAmlObjectData(uuid: String, list: List[DbModel with HasName with HasAmlId with HasModelType with HasParent]): 
  List[DbModel with HasName with HasAmlId with HasModelType with HasParent] = {
    val optElement = Await.result(amlObjectService.findOneElement(Element.queryByUuid(uuid)), Duration("3s"))
    Logger.info("optElement: " + optElement)
    val optInterface = optElement.map(element => None).
      getOrElse(Await.result(amlObjectService.findOneInterface(Interface.queryByUuid(uuid)),
        Duration("3s")))

    optElement match {
      case Some(element) => genereateAmlObjectData(element.parent, element :: list)
      case None => optInterface match {
        case Some(interface) => genereateAmlObjectData(interface.parent, interface :: list)
        case None => list
      }
    }
  }
}