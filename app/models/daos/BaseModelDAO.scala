package models.daos

import scala.concurrent.Future
import models.BaseModel
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.JsObject

trait BaseModelDAO[M <: BaseModel] {
  
  protected def collection: Future[JSONCollection]
  
  def insert(model: M): Future[Option[M]]
  
  def update(query: JsObject, update: JsObject): Future[Boolean]
  
  def remove(query: JsObject): Future[Boolean]
  
  def count(query: JsObject): Future[Int]
  
  def find(query: JsObject, page: Int, pageSize: Int): Future[List[M]]
  
  def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[M]]
}