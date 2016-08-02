package models.services

import utils.PaginateData
import models.Instruction
import models.daos.InstructionDAO
import models.daos.InstructionPartDAO
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.json.Json

class InstructionServiceImpl @Inject() (override val dao: InstructionDAO,
                                        val instructionPartDao: InstructionPartDAO)(implicit val ec: ExecutionContext)
    extends InstructionService {

}