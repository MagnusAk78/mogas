package models.daos

import scala.concurrent.Future
import models.BaseModel
import reactivemongo.play.json.collection.JSONCollection

trait BaseModelDAO[M <: BaseModel] {
  
  protected def collection: Future[JSONCollection]
  
  def insert(model: M): Future[Option[M]]
  
  def update(uuid: String, model: M): Future[Option[M]]
  
  def remove(model: M): Future[Boolean]
  
  def count(model: M): Future[Int]
  
  def find(model: M, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[M]]
  
  def findAndSort(model: M, sortBy: String, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[M]]
}