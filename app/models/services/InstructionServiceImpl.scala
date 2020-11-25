package models.services

import javax.inject.Inject
import models.daos.{InstructionDAO, InstructionPartDAO}
import models._
import play.api.libs.json.JsObject
import utils.RemoveResult
import viewdata.{ModelListData, PaginateData}

import scala.concurrent.{ExecutionContext, Future}

class InstructionServiceImpl @Inject() (val instructionDao: InstructionDAO,
  val instructionPartDao: InstructionPartDAO,
  val amlObjectService: AmlObjectService,
  val fileService: FileService)(implicit val ec: ExecutionContext)
    extends InstructionService {

  override def getInstructionList(page: Int, domain: Option[Domain] = None): Future[ModelListData[Instruction]] = {
    val selector = domain.map(f => HasConnectionTo.queryByHasConnectionTo(f.uuid)).getOrElse(DbModel.queryAll)
    findManyInstructions(selector, page, utils.DefaultValues.DefaultPageLength)
  }

  override def insertInstruction(instruction: Instruction): Future[Option[Instruction]] =
    instructionDao.insert(instruction).map(wr => if (wr.ok) Some(instruction) else None)

  override def updateInstruction(instruction: Instruction): Future[Boolean] =
    instructionDao.update(instruction).map(wr => wr.ok)

  override def removeInstruction(instruction: Instruction): Future[RemoveResult] = {
    val responses = for {
      result <- instructionDao.remove(instruction).map(success => if (success) {
        RemoveResult(true, None)
      } else {
        RemoveResult(false, Some("DAO refused to remove instruction: " + instruction.uuid))
      })
    } yield result

    responses recover {
      case e => RemoveResult(false, Some(e.getMessage()))
    }
  }

  override def findOneInstruction(query: JsObject): Future[Option[Instruction]] =
    instructionDao.find(query, 1, 1).map(_.headOption)

  override def findManyInstructions(query: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Instruction]] = {
    for {
      theList <- instructionDao.find(query, page, utils.DefaultValues.DefaultPageLength)
      count <- instructionDao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[Instruction] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }

  override def getInstructionPartList(instruction: Instruction, page: Int): Future[ModelListData[InstructionPart]] = {
    findManySortedInstructionParts(HasParent.queryByParent(instruction.uuid), OrderedModel.sortByOrderNumber,
      page, utils.DefaultValues.DefaultPageLength)
  }

  def getNextOrderNumber(instruction: Instruction): Future[Int] = {
    instructionPartDao.count(HasParent.queryByParent(instruction.uuid)).map { count => count + 1 }
  }

  override def movePartUp(instructionPart: InstructionPart): Future[Boolean] = movePart(instructionPart, -1)

  override def movePartDown(instructionPart: InstructionPart): Future[Boolean] = movePart(instructionPart, 1)

  private def movePart(instructionPart: InstructionPart, direction: Int): Future[Boolean] = {
    val result = for {
      instructionOpt <- findOneInstruction(DbModel.queryByUuid(instructionPart.parent))
      optOther <- findOneInstructionPart(HasParent.queryByParent(instructionOpt.get.uuid) ++
        OrderedModel.queryByOrderNumber(instructionPart.orderNumber + direction))
      updateOther <- updateInstructionPart(optOther.get.copy(orderNumber = optOther.get.orderNumber - direction))
      updateThis <- updateInstructionPart(instructionPart.copy(orderNumber = instructionPart.orderNumber + direction))
    } yield updateThis

    result recover {
      case e => false
    }
  }

  override def removeInstructionPart(instructionPart: InstructionPart): Future[RemoveResult] = {
    val responses = for {
      result <- instructionPartDao.remove(instructionPart).map(success => if (success) {
        RemoveResult(true, None)
      } else {
        RemoveResult(false, Some("DAO refused to remove instructionPart: " + instructionPart.uuid))
      })
    } yield result

    responses recover {
      case e => RemoveResult(false, Some(e.getMessage()))
    }
  }

  override def insertInstructionPart(instructionPart: InstructionPart): Future[Option[InstructionPart]] =
    instructionPartDao.insert(instructionPart).map(wr => if (wr.ok) Some(instructionPart) else None)

  override def updateInstructionPart(instructionPart: InstructionPart): Future[Boolean] =
    instructionPartDao.update(instructionPart).map(wr => wr.ok)

  override def findOneInstructionPart(query: JsObject): Future[Option[InstructionPart]] =
    instructionPartDao.find(query, 1, 1).map(_.headOption)

  override def findManyInstructionParts(query: JsObject, page: Int = 1, pageSize: Int = utils.DefaultValues.
    DefaultPageLength): Future[ModelListData[InstructionPart]] = for {
    theList <- instructionPartDao.find(query, page, utils.DefaultValues.DefaultPageLength)
    count <- instructionPartDao.count(query)
    il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
    vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
  } yield new ModelListData[InstructionPart] {
    override val list = theList
    override val imageList = il
    override val videoList = vl
    override val paginateData = PaginateData(page, count)
  }

  override def findManySortedInstructionParts(query: JsObject, sort: JsObject, page: Int = 1,
    pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[InstructionPart]] = {
    for {
      theList <- instructionPartDao.findAndSort(query, sort, page, utils.DefaultValues.DefaultPageLength)
      count <- instructionPartDao.count(query)
      il <- Future.sequence(theList.map(i => fileService.imageExists(i.uuid)))
      vl <- Future.sequence(theList.map(i => fileService.videoExists(i.uuid)))
    } yield new ModelListData[InstructionPart] {
      override val list = theList
      override val imageList = il
      override val videoList = vl
      override val paginateData = PaginateData(page, count)
    }
  }
}