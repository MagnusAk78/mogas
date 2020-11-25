package models.daos

import models.DbModel
import play.api.libs.json.{JsObject, Json, OWrites}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.ReadConcern
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseModelDAO[M <: DbModel](implicit ec: ExecutionContext) {

  implicit def jsObjectWriter: OWrites[JsObject] = Json.writes[JsObject]

  protected def collection: Future[JSONCollection]

  final def count(query: JsObject): Future[Int] = collection.flatMap(_.count(selector = Some(query),
    limit = None, skip = 0, hint = None, readConcern = ReadConcern.Local )).map( longCount => longCount.toInt)

  def insert(document: M): Future[WriteResult]

  def update(document: M): Future[WriteResult]

  def remove(document: M): Future[Boolean]

  def findOne(query: JsObject): Future[Option[M]]

  def find(query: JsObject, page: Int, pageSize: Int): Future[List[M]]

  def findAndSort(query: JsObject, sort: JsObject, page: Int, pageSize: Int): Future[List[M]]
}
