package models.daos

import scala.concurrent.Future
import models.BaseModel
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue

//Trait of DAO that work with JSON objects
trait ModelDAO {
    
  def insert(model: JsObject): Future[Option[JsObject]]
  
  def update(query: JsObject, update: JsObject): Future[Boolean]
  
  def remove(query: JsObject): Future[Boolean]
  
  def count(query: JsObject): Future[Int]
  
  def find(query: JsObject, page: Int, pageSize: Int): Future[List[JsObject]]
  
  def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[JsObject]]
}