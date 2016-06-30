package models.daos

/**
 * I copied this from Mohive to test, there is something wrong with it.
 */

/**
 * Copyright 2016 Mohiva Organisation (license at mohiva dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */

import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import com.mohiva.play.silhouette.api.{ AuthInfo, LoginInfo }

// This does not exist obviously
//import com.mohiva.play.silhouette.persistence.exceptions.MongoException

import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{ Format, JsObject, Json }
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.reflect.ClassTag
import play.api.Logger

/**
 * An implementation of the auth info DAO which stores the data in a MongoDB database.
 *
 * @param reactiveMongoApi The ReactiveMongo API.
 * @param config           The Play configuration.
 * @tparam A The type of the auth info to store.
 */
class MyAuthInfoDAO[A <: AuthInfo: ClassTag: Format](reactiveMongoApi: ReactiveMongoApi, config: Configuration)
  extends DelegableAuthInfoDAO[A] {

  /**
   * The name of the auth info to store.
   */
  private val authInfoName = implicitly[ClassTag[A]].runtimeClass.getSimpleName

  /**
   * The name of the collection to store the auth info.
   */
  private val collectionName = config.getString(s"silhouette.persistence.reactivemongo.collection.$authInfoName")
    .getOrElse(s"$authInfoName")
    
  Logger.info("MyAuthInfoDAO, " + collectionName + " has been created")

  /**
   * The collection to use for JSON queries.
   */
  private def jsonCollection = reactiveMongoApi.database.map(_.collection[JSONCollection](collectionName))

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo) = {
    jsonCollection.flatMap(_.find(Json.obj("_id" -> loginInfo)).projection(Json.obj("_id" -> 0)).one[A])
  }

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: A) = {
    Logger.info("MyAuthInfoDAO, add; " + loginInfo + ", " + authInfo)
    onSuccess(jsonCollection.flatMap(_.insert(merge(loginInfo, authInfo))), authInfo)
  }

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: A) = {
    updated(jsonCollection.flatMap(_.update(Json.obj("_id" -> loginInfo), merge(loginInfo, authInfo)))).map {
      case num if num > 0 => authInfo
      case _ => throw new Exception(s"Could not update $authInfoName for login info: " + loginInfo)
    }
  }

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo The auth info to save.
   * @return The saved auth info.
   */
  def save(loginInfo: LoginInfo, authInfo: A) = {
    Logger.info("MyAuthInfoDAO, save; " + loginInfo + ", " + authInfo)
    
    onSuccess(jsonCollection.flatMap(_.update(Json.obj("_id" -> loginInfo), merge(loginInfo, authInfo), upsert = true)), authInfo)
  }

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo) = {
    Logger.info("MyAuthInfoDAO, remove; " + loginInfo)
    
    onSuccess(jsonCollection.flatMap(_.remove(Json.obj("_id" -> loginInfo))), ())
  }

  /**
   * Merges the [[LoginInfo]] and the [[AuthInfo]] into one Json object.
   *
   * @param loginInfo The login info to merge.
   * @param authInfo The auth info to merge.
   * @return A Json object consisting of the [[LoginInfo]] and the [[AuthInfo]].
   */
  private def merge(loginInfo: LoginInfo, authInfo: A) =
    Json.obj("_id" -> loginInfo).deepMerge(Json.toJson(authInfo).as[JsObject])

  /**
   * Returns some result on success and None on error.
   *
   * @param result The last result.
   * @param entity The entity to return.
   * @tparam T The type of the entity.
   * @return The entity on success or an exception on error.
   */
  private def onSuccess[T](result: Future[WriteResult], entity: T): Future[T] = result.recoverWith {
    case e => Future.failed(new Exception("Got exception from MongoDB", e.getCause))
  }.map { r =>
    WriteResult.lastError(r) match {
      case Some(e) => throw new Exception(r.message, e)
      case _ => entity
    }
  }

  /**
   * Returns the number of updated documents on success and None on error.
   *
   * @param result The last result.
   * @return The number of updated documents on success or an exception on error.
   */
  private def updated(result: Future[WriteResult]): Future[Int] = result.recoverWith {
    case e => Future.failed(new Exception("Got exception from MongoDB", e.getCause))
  }.map { r =>
    WriteResult.lastError(r) match {
      case Some(e) => throw new Exception(r.message, e)
      case _ => r.n
    }
  }
}
