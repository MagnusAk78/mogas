package models.daos

import models.Instruction

trait InstructionDAO extends BaseModelDAO[Instruction] {
  override val companionObject = Instruction
}