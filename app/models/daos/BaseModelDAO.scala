package models.daos

import models.{DbModel, DbModelComp, JsonImpl}
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.{Cursor, QueryOpts, ReadConcern}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseModelDAO[M <: DbModel with JsonImpl](implicit exec: ExecutionContext,
                                                        joWrites: OWrites[M],
                                                        jsoWrites: OWrites[JsObject],
                                                        joReads: Reads[M]) {

  implicit def optionFormat[T: Format]: Format[Option[T]] = new Format[Option[T]]{
    override def reads(json: JsValue): JsResult[Option[T]] = json.validateOpt[T]

    override def writes(o: Option[T]): JsValue = o match {
      case Some(t) ⇒ implicitly[Writes[T]].writes(t)
      case None ⇒ JsNull
    }
  }


  protected def collection: Future[JSONCollection]

  protected val companionObject: DbModelComp[M]

  final def insert(model: M): Future[WriteResult] = collection.flatMap { collection =>
    collection.insert(false).one(model) }

  final def update(model: M): Future[WriteResult] = collection.flatMap(collection => {
    val query = companionObject.queryByUuid(model.uuid)

    collection.update(false).one(query, companionObject.getUpdateObject(model))
  })

  final def remove(model: M): Future[Boolean] = collection.flatMap(
    _.delete().one(companionObject.queryByUuid(model.uuid), None)
  ).map { wr => wr.ok }

  final def count(query: JsObject): Future[Int] = collection.flatMap(_.count(selector = Some(query),
    limit = None, skip = 0, hint = None, readConcern = ReadConcern.Local )).map( longCount => longCount.toInt)

  final def findOneByUuid(uuid: String): Future[Option[M]] = find(companionObject.queryByUuid(uuid), 1, 1).map(_.headOption)

  final def find(query: JsObject, page: Int, pageSize: Int): Future[List[M]] =
    collection.flatMap(_.find(query).options(QueryOpts((page - 1) * pageSize, pageSize)).cursor[M]().
      collect[List](pageSize, Cursor.FailOnError[List[M]]()))

  final def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[M]] =
    collection.flatMap(_.find(query).sort(sort).options(QueryOpts((page - 1) * pageSize, pageSize)).cursor[M]().
      collect[List](pageSize, Cursor.FailOnError[List[M]]()))
}
