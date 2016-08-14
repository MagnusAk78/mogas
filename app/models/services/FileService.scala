package models.services

import play.api.libs.json.JsString
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.ReadFile
import reactivemongo.play.json.JSONSerializationPack
import scala.concurrent.Future
import models.daos.FileDAO
import models.daos.FileDAO.JSONReadFile
import reactivemongo.api.gridfs.GridFS
import play.api.mvc.Result
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsObject
import utils.RemoveResult

trait FileService {

  val dao: FileDAO

  def remove(uuid: String): Future[RemoveResult]

  def find(uuid: String): Future[Cursor[JSONReadFile]]

  def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]]

  def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile]

  def updateMetadata(fileUuid: String, metadata: JsObject): Future[Boolean]

  def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T]

  def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T

  def imageExists(uuid: String): Future[Boolean]

  def videoExists(uuid: String): Future[Boolean]
}