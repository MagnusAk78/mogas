package models.services

import models.daos.FileDAO
import models.daos.FileDAO.JSONReadFile
import play.api.libs.json.JsObject
import play.modules.reactivemongo.JSONFileToSave
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.GridFS
import reactivemongo.play.json.JSONSerializationPack
import utils.RemoveResult

import scala.concurrent.Future

trait FileService {

  val dao: FileDAO

  def remove(uuid: String): Future[RemoveResult]

  def find(uuid: String): Future[Cursor[JSONReadFile]]

  def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]]

  def save(inputStream: java.io.InputStream, fileToSave: JSONFileToSave): Future[JSONReadFile]

  def updateMetadata(fileUuid: String, metadata: JsObject): Future[Boolean]

  def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T]

  def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T

  def imageExists(uuid: String): Future[Boolean]

  def videoExists(uuid: String): Future[Boolean]
  
  def amlFiles(uuid: String): Future[List[JSONReadFile]]
}