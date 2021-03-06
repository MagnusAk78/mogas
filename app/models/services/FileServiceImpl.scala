package models.services

import models.daos.FileDAO
import models.daos.FileDAO.JSONReadFile
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.GridFS
import play.api.mvc.Result
import reactivemongo.play.json.JSONSerializationPack
import play.modules.reactivemongo.JSONFileToSave
import play.api.libs.json.JsObject
import utils.RemoveResult
import play.api.libs.iteratee._

class FileServiceImpl @Inject() (override val dao: FileDAO)(implicit val ec: ExecutionContext) extends FileService {

  override def remove(uuid: String): Future[RemoveResult] =
    dao.remove(uuid).map(success => if (success) {
      RemoveResult(true, None)
    } else {
      RemoveResult(false, Some("DAO refused to remove file: " + uuid))
    })

  override def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]] = dao.findByQuery(query)

  override def find(uuid: String): Future[Cursor[JSONReadFile]] = dao.find(uuid)

  override def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile] = dao.save(enumerator, fileToSave)

  override def updateMetadata(fileUuid: String, metadata: JsObject): Future[Boolean] = dao.updateMetadata(fileUuid, metadata)

  override def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T] = dao.withAsyncGfs(func)

  override def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T = dao.withSyncGfs(func)

  override def imageExists(uuid: String): Future[Boolean] = dao.findByQuery(models.Images.getQueryAllImages(uuid)).
    flatMap { cursor => cursor.headOption }.flatMap { opt =>
      opt.map(file => Future.successful(true)).
        getOrElse(Future.successful(false))
    }

  def videoExists(uuid: String): Future[Boolean] = dao.findByQuery(models.Videos.getQueryAllVideos(uuid)).
    flatMap { cursor => cursor.headOption }.flatMap { opt =>
      opt.map(file => Future.successful(true)).
        getOrElse(Future.successful(false))
    }
    
  def amlFiles(uuid: String): Future[List[JSONReadFile]] = {
    dao.findByQuery(models.AmlFiles.getQueryAllAmlFiles(uuid)).flatMap { c => c.toList(100, true) }
  }
}