package models.services

import models.BaseModel
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import models.daos.BaseModelDAO
import play.api.libs.json.JsObject
import play.api.Logger

trait BaseModelService[M <: BaseModel, D <: BaseModelDAO[M]] {
  
  protected val dao: D
  
  final def insert(model: M): Future[Option[M]] = dao.insert(model)
  
  final def update(query: JsObject, update: JsObject): Future[Boolean] = {
    
    Logger.info("BaseModelService.update query: " + query)
    
    Logger.info("BaseModelService.update update: " + update)
    
    dao.update(query, update)
  }
  
  final def remove(query: JsObject): Future[Boolean] = dao.remove(query)
  
  final def count(query: JsObject): Future[Int] = {
    
    Logger.info("BaseModelService.find count: " + query)
    
    dao.count(query)
  }
  
  final def findOne(query: JsObject)(implicit executionContext: ExecutionContext): Future[Option[M]] = {
    
    Logger.info("BaseModelService.findOne query: " + query)    
   
    dao.find(query, 1, 1).map(_.headOption)
  }
  
  final def find(query: JsObject, page: Int = 1, pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] = {
    
    Logger.info("BaseModelService.find query: " + query)
    Logger.info("BaseModelService.find page: " + page)
    Logger.info("BaseModelService.find pageSize: " + pageSize)   
   
    dao.find(query, page, pageSize)
  }
  
  final def findAndSort(query: JsObject, sort: JsObject, page: Int = 1, pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] =
    dao.findAndSort(query, sort, page, pageSize)
}