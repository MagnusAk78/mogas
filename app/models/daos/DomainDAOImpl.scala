package models.daos

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import javax.inject.Inject
import models.Domain
import play.api.Logger
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection._
import reactivemongo.api.QueryOpts

class DomainDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends DomainDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("domains"))
}