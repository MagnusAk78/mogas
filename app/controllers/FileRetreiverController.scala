package controllers

import javax.inject._

import org.joda.time.DateTime

import scala.concurrent.{ Await, Future, duration }, duration.Duration

import play.api.Logger

import models.services.OrganisationService
import models.services.UserService

import forms.OrganisationForm

import play.api.mvc.{ Action, Controller, Request }
import play.api.libs.json.{ Json, JsObject, JsString }

import reactivemongo.api.gridfs.{ GridFS, ReadFile }

import play.modules.reactivemongo.{
  MongoController,
  ReactiveMongoApi,
  ReactiveMongoComponents
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import models.Organisation
import models.User
import akka.stream.Materializer
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.i18n.Messages
import scala.concurrent.ExecutionContext
import com.mohiva.play.silhouette.api.Silhouette
import utils.auth.DefaultEnv
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry

@Singleton
class FileRetreiverController @Inject() (
  val messagesApi: MessagesApi,
  val silhouette: Silhouette[DefaultEnv],
  val organisationService: OrganisationService,
  val userService: UserService,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  //private val gridFS = reactiveMongoApi.gridFS
  private val gridFS = for {
    fs <- reactiveMongoApi.database.map(db =>
      GridFS[JSONSerializationPack.type](db))
    _ <- fs.ensureIndex().map { index =>
      // let's build an index on our gridfs chunks collection if none
      Logger.info(s"Checked index, result is $index")
    }
  } yield fs

  def getImage(imageUuid: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { request =>
    gridFS.flatMap { fs =>
      // find the matching attachment, if any, and streams it to the client
      val file = fs.find[JsObject, JSONReadFile](Json.obj("_id" -> JsString(imageUuid)))

      request.getQueryString("inline") match {
        case Some("true") =>
          serve[JsString, JSONReadFile](fs)(file, CONTENT_DISPOSITION_INLINE)

        case _ => serve[JsString, JSONReadFile](fs)(file)
      }
    }
  }
}
