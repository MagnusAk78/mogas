package models.daos

import models.daos.FileDAO.JSONReadFile
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsObject, JsValue}
import play.modules.reactivemongo.JSONFileToSave
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import reactivemongo.play.json.JSONSerializationPack

import scala.concurrent.Future

trait FileDAO {
  
  def remove(uuid: String): Future[Boolean]
    
  def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]]
  
  def find(uuid: String): Future[Cursor[JSONReadFile]]
  
  def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile]
  
  def updateMetadata(fileUuid:String, metadata: JsObject): Future[Boolean]
  
  def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T]
  
  def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T
}

object FileDAO {
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsValue]
  
  implicit def optFileToString(file: JSONReadFile): String = "JSONReadFile id:" + file.id 
}