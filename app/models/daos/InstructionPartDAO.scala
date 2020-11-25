package models.daos

import javax.inject.Inject
import models.{DbModel, InstructionPart}
import play.api.libs.json.JsObject
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection._

import scala.concurrent.{ExecutionContext, Future}

trait InstructionPartDAO extends BaseModelDAO[InstructionPart]

class InstructionPartDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext)
  extends InstructionPartDAO {
  protected override def collection: Future[JSONCollection] = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("instructionParts"))

  override def insert(ip: InstructionPart): Future[WriteResult] = collection.flatMap(_.insert.one(ip))

  override def update(ip: InstructionPart): Future[WriteResult] = collection.flatMap(collection => {
    collection.update(false).one(DbModel.queryByUuid(ip.uuid), ip)
  })

  override def remove(ip: InstructionPart): Future[Boolean] =
    collection.flatMap(_.delete().one(DbModel.queryByUuid(ip.uuid), None)).map { wr => wr.ok }

  override def findOne(query: JsObject): Future[Option[InstructionPart]] = collection.flatMap(
    _.find(query, None).cursor[InstructionPart]().headOption)

  override def find(query: JsObject, page: Int, pageSize: Int): Future[List[InstructionPart]] =
    collection.flatMap(_.find(query, None).skip((page - 1) * pageSize).cursor[InstructionPart]().
      collect[List](pageSize, Cursor.FailOnError[List[InstructionPart]]()))

  override def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[InstructionPart]] =
    collection.flatMap(_.find(query, None).sort(sort).skip((page - 1) * pageSize).cursor[InstructionPart]().
      collect[List](pageSize, Cursor.FailOnError[List[InstructionPart]]()))
}