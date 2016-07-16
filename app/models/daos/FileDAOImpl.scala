package models.daos

import scala.annotation.implicitNotFound
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import FileDAO.JSONReadFile
import javax.inject.Inject
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.modules.reactivemongo.MongoController.readFileReads
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.ReactiveMongoComponents
import reactivemongo.api.Cursor
import reactivemongo.api.gridfs.GridFS
import reactivemongo.play.json._
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.JSONFileToSave
import reactivemongo.api.gridfs.ReadFile
import play.api.Logger
import reactivemongo.api.commands.WriteResult

class FileDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) 
  extends FileDAO with ReactiveMongoComponents {
    
  private val asyncGridFS = reactiveMongoApi.asyncGridFS
  
  private def syncGridFS = Await.result(asyncGridFS, Duration("5s"))

  override def remove(uuid: String): Future[Boolean] = 
    asyncGridFS.flatMap(_.remove(JsString(uuid)).map { wr => wr.n == 1 })
        
  override def findByQuery(query: JsObject): Future[Cursor[JSONReadFile]] =
    asyncGridFS.map(_.find[JsObject, JSONReadFile](query))
    
  override def find(uuid: String): Future[Cursor[JSONReadFile]] = 
    asyncGridFS.map(_.find[JsObject, JSONReadFile](Json.obj("_id" -> JsString(uuid))))
    
  override def save(enumerator: Enumerator[Array[Byte]], fileToSave: JSONFileToSave): Future[JSONReadFile] = {
    asyncGridFS.flatMap(_.save(enumerator, fileToSave))
  }
    
  override def withAsyncGfs[T](func: (GridFS[JSONSerializationPack.type] => Future[T])): Future[T] = asyncGridFS.flatMap(func(_))
  
  override def withSyncGfs[T](func: (GridFS[JSONSerializationPack.type] => T)): T = func(syncGridFS)
}
