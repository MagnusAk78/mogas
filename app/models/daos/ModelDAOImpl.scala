package models.daos

import com.mohiva.play.silhouette.api.LoginInfo

import scala.collection.mutable
import scala.concurrent.Future
import play.modules.reactivemongo.ReactiveMongoApi
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import play.api.libs.json._
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import models.Organisation
import reactivemongo.api.QueryOpts
import play.api.Logger
import models.BaseModel

//Implementation of ModelDAO that use ReactiveMongo and MongoDB
class ModelDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends ModelDAO {
  
  import play.api.libs.json._

  private def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("models"))
    
  override def insert(model: JsObject): Future[Option[JsObject]] = {
    Logger.info("ModelDAOImpl.insert model:" +  Json.prettyPrint(model))
    
    collection.flatMap { collection => collection.insert(model) }.map { wr => if(wr.ok) Some(model) else None }
  }
  
  override def update(query: JsObject, update: JsObject): Future[Boolean] = collection.flatMap ( collection => 
      collection.update(query, update)).map(wr => wr.ok)

  override def remove(query: JsObject): Future[Boolean] = collection.flatMap(_.remove(query).map { wr => wr.ok })
    
  override def count(query: JsObject): Future[Int] = collection.flatMap(_.count(Some(query)))

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[JsObject]] =
    collection.flatMap(_.find(query).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[JsObject]().collect[List](pageSize))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[JsObject]] =
    collection.flatMap(_.find(query).sort(sort).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[JsObject]().collect[List](pageSize))
}
