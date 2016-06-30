package models.daos

import scala.concurrent.Future
import models.BaseModel
import models.ModelKey
import reactivemongo.play.json.collection.JSONCollection


trait BaseModelDAO[M <: BaseModel] {
  
  protected def collection: Future[JSONCollection]
  
  def save(model: M): Future[Option[M]]
  
  def remove(model: M): Future[Boolean]
  
  def find(model: M, maxDocs: Int = 0): Future[List[M]]
  
  def findAndSort(model: M, sortBy: ModelKey, ascending: Boolean, maxDocs: Int = 0): Future[List[M]]
}