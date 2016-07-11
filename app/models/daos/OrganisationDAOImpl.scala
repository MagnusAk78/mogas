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
  
  override def update(uuid: String, newOrganisation: Organisation): Future[Option[Organisation]] = {
    
    val selector = JsObject(Seq(Organisation.KeyUUID -> JsString(uuid)))
    
    val updateJsValue = JsObject(Seq("$set" -> Organisation.OrganisationWrites.writes(newOrganisation)))
    
    collection.flatMap ( collection => 
      collection.update(selector, updateJsValue)).map(wr => if(wr.ok) Some(newOrganisation) else None)
  }

  override def remove(organisation: Organisation): Future[Boolean] = collection.flatMap(_.remove(organisation).map { wr => wr.ok })
    
  override def count(organisation: Organisation): Future[Int] = collection.flatMap(_.count(Some(Organisation.OrganisationWrites.writes(organisation))))

  override def find(organisation: Organisation, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[Organisation]] =
    collection.flatMap(_.find(organisation).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[Organisation]().collect[List](maxDocs))

  override def findAndSort(organisation: Organisation, sortBy: String, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[Organisation]] =
    collection.flatMap(_.find(organisation).sort(DaoHelper.getSortByJsObject(sortBy, ascending)).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[Organisation]().collect[List](maxDocs))
}
