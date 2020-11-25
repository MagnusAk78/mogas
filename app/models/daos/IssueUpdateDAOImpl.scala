package models.daos

import javax.inject.Inject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

class IssueUpdateDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
    extends IssueUpdateDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("issueUpdates"))
}