package models.services

import models.{Domain, Instruction, InstructionPart}
import play.api.libs.json.JsObject
import utils.RemoveResult
import viewdata.ModelListData

import scala.concurrent.Future

trait InstructionService {

  def getInstructionList(page: Int, domain: Option[Domain] = None): Future[ModelListData[Instruction]]

  def insertInstruction(model: Instruction): Future[Option[Instruction]]

  def updateInstruction(model: Instruction): Future[Boolean]

  def removeInstruction(model: Instruction): Future[RemoveResult]

  def findOneInstruction(query: JsObject): Future[Option[Instruction]]

  def findManyInstructions(query: JsObject, page: Int = 1,
                           pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[Instruction]]

  def getInstructionPartList(instruction: Instruction, page: Int): Future[ModelListData[InstructionPart]]

  def getNextOrderNumber(instruction: Instruction): Future[Int]

  def movePartUp(instructionPart: InstructionPart): Future[Boolean]

  def movePartDown(instructionPart: InstructionPart): Future[Boolean]

  def removeInstructionPart(instructionPart: InstructionPart): Future[RemoveResult]

  def insertInstructionPart(model: InstructionPart): Future[Option[InstructionPart]]

  def updateInstructionPart(model: InstructionPart): Future[Boolean]

  def findOneInstructionPart(query: JsObject): Future[Option[InstructionPart]]

  def findManyInstructionParts(query: JsObject, page: Int = 1,
                               pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[InstructionPart]]

  def findManySortedInstructionParts(query: JsObject, sort: JsObject, page: Int = 1,
                                     pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[ModelListData[InstructionPart]]
}