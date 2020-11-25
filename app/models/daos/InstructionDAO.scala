package models.daos

import javax.inject.Inject
import models.{DbModel, Instruction}
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

trait InstructionDAO extends BaseModelDAO[Instruction]

class InstructionDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
  extends InstructionDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("instructions"))

  override def insert(document: Instruction): Future[WriteResult] = collection.flatMap(_.insert.one(document))

  override def update(document: Instruction): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(DbModel.queryByUuid(document.uuid), document)
  })

  override def remove(document: Instruction): Future[Boolean] =
    collection.flatMap(_.delete().one(DbModel.queryByUuid(document.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[Instruction]] = collection.flatMap(
    _.find(query, None).cursor[Instruction]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[Instruction]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[Instruction]().
      collect[List](pageSize, Cursor.FailOnError[List[Instruction]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[Instruction]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[Instruction]().
      collect[List](pageSize, Cursor.FailOnError[List[Instruction]]()))
}