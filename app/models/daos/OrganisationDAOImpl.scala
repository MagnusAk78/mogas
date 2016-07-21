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
class OrganisationDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends OrganisationDAO {

  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("organisations"))
}