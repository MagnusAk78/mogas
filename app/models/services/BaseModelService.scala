package models.services

import models.BaseModel
import models.ModelKey
import scala.concurrent.Future
import models.daos.BaseModelDAO

trait BaseModelService[M <: BaseModel, D <: BaseModelDAO[M]] {
  
  protected val dao: D
  
  final def save(model: M): Future[Option[M]] = dao.save(model)
  
  final def remove(model: M): Future[Boolean] = dao.remove(model)
  
  final def find(model: M, maxDocs: Int = 0): Future[List[M]] = dao.find(model, maxDocs)
  
  final def findAndSort(model: M, sortBy: ModelKey, ascending: Boolean, maxDocs: Int = 0): Future[List[M]] = 
    dao.findAndSort(model, sortBy, ascending, maxDocs)
}