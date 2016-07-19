package models.services

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import models.BaseModel
import models.daos.BaseModelDAO
import play.api.Logger
import play.api.libs.json.JsObject
import play.api.libs.json.OWrites
import play.api.libs.json.Reads

trait BaseModelService[M <: BaseModel] {
  
  implicit val ec: ExecutionContext
      
  protected val dao: BaseModelDAO[M]
  
  final def insert(model: M): Future[Option[M]] = dao.insert(model)
  
  final def update(query: JsObject, update: JsObject): Future[Boolean] = {
    
    Logger.info("BaseModelService.update query: " + query)
    
    Logger.info("BaseModelService.update update: " + update)
    
    dao.update(query, update)
  }
  
  final def count(query: JsObject): Future[Int] = {
    
    Logger.info("BaseModelService.find count: " + query)
    
    dao.count(query)
  }
  
  final def findOneByUuid(uuid: String): Future[Option[M]] = uuid match {
    case models.UuidNotSet => Future.successful(None)
    case _ => {
      val query = BaseModel.uuidQuery(uuid)
    
      Logger.info("BaseModelService.findOneByUuid query: " + query)    
   
      dao.find(query, 1, 1).map(_.headOption)
    }
  }
  
  final def findOne(query: JsObject): Future[Option[M]] = {
    
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