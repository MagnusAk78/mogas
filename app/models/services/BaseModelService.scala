package models.services

import models.BaseModel
import scala.concurrent.Future
import models.daos.BaseModelDAO

trait BaseModelService[M <: BaseModel, D <: BaseModelDAO[M]] {
  
  protected val dao: D
  
  final def insert(model: M): Future[Option[M]] = dao.insert(model)
  
  final def update(uuid: String, model: M): Future[Option[M]] = dao.update(uuid, model)
  
  final def remove(model: M): Future[Boolean] = dao.remove(model)
  
  final def count(model: M): Future[Int] = dao.count(model)
  
  final def find(model: M, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[M]] =
    dao.find(model, pageNumber, numberPerPage, maxDocs)
  
  final def findAndSort(model: M, sortBy: String, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[M]] =
    dao.findAndSort(model, sortBy, ascending, pageNumber, numberPerPage, maxDocs)
}