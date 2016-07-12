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

/**
 * Give access to the organisation object.
 */
class OrganisationDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends OrganisationDAO {
  
  import play.api.libs.json._

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("organisations"))
    
  override def insert(organisation: Organisation): Future[Option[Organisation]] = {
    Logger.info("OrganisationDAOImpl.insert organisation:" +  organisation)
    
    collection.flatMap { collection => collection.insert(organisation) }.map { wr => if(wr.ok) Some(organisation) else None }
  }
  
  override def update(query: JsObject, update: JsObject): Future[Boolean] = collection.flatMap ( collection => 
      collection.update(query, update)).map(wr => wr.ok)

  override def remove(query: JsObject): Future[Boolean] = collection.flatMap(_.remove(query).map { wr => wr.ok })
    
  override def count(query: JsObject): Future[Int] = collection.flatMap(_.count(Some(query)))

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Organisation]] =
    collection.flatMap(_.find(query).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[Organisation]().collect[List](pageSize))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Organisation]] =
    collection.flatMap(_.find(query).sort(sort).options(QueryOpts((page - 1)*pageSize, pageSize)).cursor[Organisation]().collect[List](pageSize))
}
