package models.services

import models.BaseModel
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import models.daos.ModelDAO
import play.api.libs.json.JsObject
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.OWrites
import play.api.libs.json.Reads

trait BaseModelService[M <: BaseModel] {
  
  implicit val ec: ExecutionContext
  
  implicit val joWrites: OWrites[M]
  
  implicit val joReads: Reads[M]  
    
  protected val dao: ModelDAO
  
  final def insert(model: M): Future[Option[M]] = dao.insert(joWrites.writes(model)).map(_.map { jsObj => jsObj.as[M] })
  
  final def update(query: JsObject, update: JsObject): Future[Boolean] = {
    
    Logger.info("BaseModelService.update query: " + query)
    
    Logger.info("BaseModelService.update update: " + update)
    
    dao.update(query, update)
  }

  def remove(model: M): Future[RemoveResult] = dao.remove(model.uuidQuery).map(success => if (success) {
    RemoveResult(true, None)
  } else {
    RemoveResult(false, Some("DAO refused to remove: " + model.uuid))
  })
  
  final def count(query: JsObject): Future[Int] = {
    
    Logger.info("BaseModelService.find count: " + query)
    
    dao.count(query)
  }
  
  final def findOneByUuid(uuid: String): Future[Option[M]] = {
    val query = BaseModel.uuidQuery(uuid)
    
    Logger.info("BaseModelService.findOneByUuid query: " + query)    
   
    dao.find(query, 1, 1).map(_.headOption.map( jsObj => jsObj.as[M] ))
  }
  
  final def findOne(query: JsObject): Future[Option[M]] = {
    
    Logger.info("BaseModelService.findOne query: " + query)    
   
    dao.find(query, 1, 1).map(_.headOption.map( jsObj => jsObj.as[M] ))
  }
  
  final def find(query: JsObject, page: Int = 1, pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] = {
    
    Logger.info("BaseModelService.find query: " + query)
    Logger.info("BaseModelService.find page: " + page)
    Logger.info("BaseModelService.find pageSize: " + pageSize)       
       
    dao.find(query, page, pageSize).map(_.map{ 
        jsObj => {
        Logger.info("jsObj: " + jsObj)    
          jsObj.as[M]
        }
        })
  }
  
  final def findAndSort(query: JsObject, sort: JsObject, page: Int = 1, pageSize: Int = utils.DefaultValues.DefaultPageLength): Future[List[M]] =
    dao.findAndSort(query, sort, page, pageSize).map(_.map{ jsObj => jsObj.as[M] })
}