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
import models.ModelKey
import reactivemongo.api.QueryOpts

/**
 * Give access to the organisation object.
 */
class OrganisationDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends OrganisationDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("organisations"))
  
  override def save(organisation: Organisation): Future[Option[Organisation]] = organisation.id match {
    case Some(id) => {
      //This has an id, it is probably an update
      val selector = Organisation(id = Some(id))

      collection.flatMap(_.update(selector = selector, organisation).map { wr =>
        wr.ok match {
          case true  => Some(organisation)
          case false => None
        }
      })
    }
    case None => {
      //This has no id, it is an insert
      collection.flatMap(_.insert(organisation).map { wr =>
        wr.ok match {
          case true  => Some(organisation)
          case false => None
        }
      })
    }
  }

  override def remove(organisation: Organisation): Future[Boolean] = collection.flatMap(_.remove(organisation).map { wr => wr.ok })
    
  override def count(organisation: Organisation): Future[Int] = collection.flatMap(_.count(Some(Organisation.OrganisationWrites.writes(organisation))))

  override def find(organisation: Organisation, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[Organisation]] =
    collection.flatMap(_.find(organisation).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[Organisation]().collect[List](maxDocs))

  override def findAndSort(organisation: Organisation, sortBy: ModelKey, ascending: Boolean, pageNumber: Int = 1, numberPerPage: Int = 20, maxDocs: Int = 0): Future[List[Organisation]] =
    collection.flatMap(_.find(organisation).sort(DaoHelper.getSortByJsObject(sortBy.value, ascending)).options(QueryOpts((pageNumber - 1)*numberPerPage, numberPerPage)).cursor[Organisation]().collect[List](maxDocs))
}
