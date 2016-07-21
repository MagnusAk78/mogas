package models.daos

import scala.concurrent.Future

import play.api.libs.json.JsString
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.play.json.JSONSerializationPack
import reactivemongo.api.gridfs.GridFS
import play.api.mvc.Result
import play.api.libs.json.JsValue
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.iteratee.Enumerator
import FileDAO.JSONReadFile
import play.api.libs.json.JsObject

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