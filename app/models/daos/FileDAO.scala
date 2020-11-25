package models.daos

import javax.inject.Inject
import models.AmlFiles
import models.daos.FileDAO.JSONReadFile
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.modules.reactivemongo.MongoController.readFileReads
import play.modules.reactivemongo.{JSONFileToSave, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import reactivemongo.play.json.{JSONSerializationPack, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait FileDAO {

  def remove(uuid: String): Future[Boolean]

  def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]]

  def find(uuid: String): Future[Cursor[JSONReadFile]]

  def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile]

  def updateMetadata(fileUuid: String, metadata: JsObject): Future[Boolean]

  def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T]

  def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T
}

object FileDAO {
  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsValue]

  implicit def optFileToString(file: JSONReadFile): String = "JSONReadFile id:" + file.id
}

class FileDAOImpl @Inject()(val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
  extends FileDAO with ReactiveMongoComponents {

  private val asyncGridFS = reactiveMongoApi.asyncGridFS

  private def syncGridFS = Await.result(asyncGridFS, Duration("5s"))

  override def remove(uuid: String): Future[Boolean] =
    asyncGridFS.flatMap(_.remove(JsString(uuid)).map { wr => wr.n == 1 })

  override def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]] =
    asyncGridFS.map(_.find[JsObject, JSONReadFile](query))

  override def find(uuid: String): Future[Cursor[JSONReadFile]] =
    asyncGridFS.map(_.find[JsObject, JSONReadFile](Json.obj("_id" -> JsString(uuid))))

  override def updateMetadata(fileUuid: String, metadata: JsObject): Future[Boolean] =
    asyncGridFS.flatMap(_.files.update(Json.obj("_id" -> JsString(fileUuid)),
      Json.obj("$set" -> Json.obj(AmlFiles.KeyMetadata -> metadata))).map(ur => ur.ok))


  override def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile] = {
    asyncGridFS.flatMap(_.save(enumerator, fileToSave))
  }

  override def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T] =
    asyncGridFS.flatMap(func(_))

  override def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T = func(syncGridFS)
}