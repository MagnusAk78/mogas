package controllers

import javax.inject.Inject

import scala.concurrent.Future

import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.json._
import play.api.i18n.{ I18nSupport, MessagesApi }

// Reactive Mongo imports
import reactivemongo.api.Cursor

import play.modules.reactivemongo.{ 
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}

// BSON-JSON conversions/collection
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import services.RandomNameService

import scala.concurrent.ExecutionContext

/*
 * Example using ReactiveMongo + Play JSON library.
 *
 * There are two approaches demonstrated in this controller:
 * - using JsObjects directly
 * - using case classes that can be turned into JSON using Reads and Writes.
 *
 * This controller uses case classes and their associated Reads/Writes
 * to read or write JSON structures.
 *
 * Instead of using the default Collection implementation (which interacts with
 * BSON structures + BSONReader/BSONWriter), we use a specialized
 * implementation that works with JsObject + Reads/Writes.
 *
 * Of course, you can still use the default Collection implementation
 * (BSONCollection.) See ReactiveMongo examples to learn how to use it.
 */

class ReactiveMongoController @Inject()(val reactiveMongoApi: ReactiveMongoApi,
    randomNameService: RandomNameService, val messagesApi: MessagesApi)(implicit exec: ExecutionContext, 
        implicit val webJarAssets: WebJarAssets)
  extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {
  
  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def personsCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("persons"))

  // ------------------------------------------ //
  // Using case classes + JSON Writes and Reads //
  // ------------------------------------------ //
  import play.api.data.Form
  import models._
  import models.JsonFormats._

  def create = Action.async {
    val reactiveTestUser = ReactiveTestUser(None, randomNameService.firstName(), randomNameService.lastName(), List(
      Feed("Slashdot news", "http://slashdot.org/slashdot.rdf")))
    // insert the reactiveTestUser
    val futureResult = personsCollection.flatMap { _.insert(reactiveTestUser) }
    // when the insert is performed, send a OK 200 result
    futureResult.map(_ => Ok)
  }

  def createFromJson = Action.async(parse.json) { request =>
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
    request.body.validate[ReactiveTestUser].map { reactiveTestUser =>
      // `reactiveTestUser` is an instance of the case class `models.ReactiveTestUser`
      personsCollection.flatMap { _.insert(reactiveTestUser) }.map { lastError =>
        Logger.debug(s"Successfully inserted with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findByName(lastName: String) = Action.async {
    // let's do our query, find and sort all people with lastName `lastName`
    val peopleCursor: Future[Cursor[ReactiveTestUser]] = personsCollection.map { collection: JSONCollection => collection.find(Json.obj("lastName" -> lastName)).sort(Json.obj("created" -> -1)).cursor[ReactiveTestUser]() }
        
    // gather all the JsObjects in a list
    val futureUsersList: Future[List[ReactiveTestUser]] = peopleCursor.flatMap { cursor => cursor.collect[List]() }

    // everything's ok! Let's reply with the array
    futureUsersList.map { persons: List[ReactiveTestUser] =>
      Ok(persons.toString)
    }
  }
  
  def listAll = Action.async {
    // let's do our query, find and sort all people with lastName `lastName`
    val peopleCursor: Future[Cursor[ReactiveTestUser]] = personsCollection.map { collection: JSONCollection => collection.find(Json.obj()).sort(Json.obj("created" -> -1)).cursor[ReactiveTestUser]() }
        
    // gather all the JsObjects in a list
    val futureUsersList: Future[List[ReactiveTestUser]] = peopleCursor.flatMap { cursor => cursor.collect[List]() }

    // everything's ok! Let's reply with the array
    futureUsersList.map { persons: List[ReactiveTestUser] =>
      Ok(persons.toString)
    }
  }
}