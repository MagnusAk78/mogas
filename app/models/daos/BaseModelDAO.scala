package models.daos

import scala.concurrent.Future
import models.DbModel
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.OWrites
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.JSONSerializationPack
import play.modules.reactivemongo.json._
import reactivemongo.api.QueryOpts
import models.DbModelComp
import models.DbModel

abstract class BaseModelDAO[M <: DbModel](implicit exec: ExecutionContext, implicit val joWrites: OWrites[M],
                                          implicit val joReads: Reads[M]) {

  protected def collection: Future[JSONCollection]

  protected val companionObject: DbModelComp[M]

  final def insert(model: M): Future[WriteResult] = collection.flatMap { collection => collection.insert(model) }

  final def update(model: M): Future[WriteResult] = collection.flatMap(collection => {
    val query = companionObject.queryByUuid(model.uuid)

    collection.update(query, companionObject.getUpdateObject(model))
  })

  final def remove(model: M): Future[Boolean] = collection.flatMap(_.remove(companionObject.queryByUuid(model.uuid)).
    map { wr => wr.ok })

  final def count(query: JsObject): Future[Int] = collection.flatMap(_.count(Some(query)))

  final def findOneByUuid(uuid: String): Future[Option[M]] = find(companionObject.queryByUuid(uuid), 1, 1).map(_.headOption)

  final def find(query: JsObject, page: Int, pageSize: Int): Future[List[M]] =
    collection.flatMap(_.find(query).options(QueryOpts((page - 1) * pageSize, pageSize)).cursor[M]().
      collect[List](pageSize))

  final def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[M]] =
    collection.flatMap(_.find(query).sort(sort).options(QueryOpts((page - 1) * pageSize, pageSize)).cursor[M]().
      collect[List](pageSize))
}
