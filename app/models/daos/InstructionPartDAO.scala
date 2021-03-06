package models.daos

import models.InstructionPart

trait InstructionPartDAO extends BaseModelDAO[InstructionPart] {
  override val companionObject = InstructionPart
}