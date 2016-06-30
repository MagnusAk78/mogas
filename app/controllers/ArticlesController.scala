package controllers

import javax.inject._

import org.joda.time.DateTime

import scala.concurrent.{ Await, Future, duration }, duration.Duration

import play.api.Logger

import play.api.mvc.{ Action, Controller, Request }
import play.api.libs.json.{ Json, JsObject, JsString }

import reactivemongo.api.gridfs.{ GridFS, ReadFile }

import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}

import reactivemongo.play.json._
import reactivemongo.play.json.collection._

import models.Article
import models.Article._
import akka.stream.Materializer
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import scala.concurrent.ExecutionContext
import models.services.ArticleService
import com.mohiva.play.silhouette.api.Silhouette
import utils.auth.DefaultEnv
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.ModelKey

@Singleton
class ArticlesController @Inject()(
    val silhouette: Silhouette[DefaultEnv],
    val messagesApi: MessagesApi,
    val articleService: ArticleService,
    val reactiveMongoApi: ReactiveMongoApi,
    val socialProviderRegistry: SocialProviderRegistry,
    implicit val webJarAssets: WebJarAssets,
    implicit val materializer: Materializer)(implicit exec: ExecutionContext) extends Controller with I18nSupport with MongoController 
    with ReactiveMongoComponents  {
  
  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]

  // get the collection 'articles'
  /*
  def collection = reactiveMongoApi.database.
    map(_.collection[JSONCollection]("articles"))
    * 
    */

  // a GridFS store named 'attachments'
  //val gridFS = GridFS(db, "attachments")
  private val gridFS = for {
    fs <- reactiveMongoApi.database.map(db =>
      GridFS[JSONSerializationPack.type](db))
    _ <- fs.ensureIndex().map { index =>
      // let's build an index on our gridfs chunks collection if none
      Logger.info(s"Checked index, result is $index")
    }
  } yield fs

  // list all articles and sort them
  val index = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    // get a sort document (see getSort method for more information)
    
    val sort = getSort(request)
    
    val foundList = sort match {
      case Some(sortValues) => articleService.findAndSort(Article(), sortValues._1, sortValues._2)
      case None => articleService.find(Article())
    }
        
    val activeSort = request.queryString.get("sort").
      flatMap(_.headOption).getOrElse("none")


    // build (asynchronously) a list containing all the articles
    foundList.map { articles =>
      Ok(views.html.articles(Some(request.identity), articles, activeSort))
    }.recover {
      case e =>
        e.printStackTrace()
        BadRequest(e.getMessage())
    }
  }

  def showCreationForm = silhouette.SecuredAction(AlwaysAuthorized()) { request =>
    Ok(views.html.editArticle(Some(request.identity), None, Article.form, None))
  }

  def showEditForm(id: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { request =>
    // get the documents having this id (there will be 0 or 1 result)
    def futureArticle = articleService.find(new Article(id = Some(id))).map { _.headOption }

    // ... so we get optionally the matching article, if any
    // let's use for-comprehensions to compose futures
    for {
      // get a future option of article
      maybeArticle <- futureArticle
      // if there is some article, return a future of result with the article and its attachments
      fs <- gridFS
      result <- maybeArticle.map { article =>
        // search for the matching attachments
        // find(...).toList returns a future list of documents
        // (here, a future list of ReadFileEntry)
        fs.find[JsObject, JSONReadFile](
          Json.obj("article" -> article.id.get)).collect[List]().map { files =>

          @inline def filesWithId = files.map { file => file.id -> file }
          
          Ok(views.html.editArticle(Some(request.identity), Some(id),
            Article.form.fill(article), Some(filesWithId)))
        }
      }.getOrElse(Future.successful(NotFound))
    } yield result
  }

  def create = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Article.form.bindFromRequest.fold(
      errors => Future.successful(
        Ok(views.html.editArticle(Some(request.identity), None, errors, None))),

      // if no error, then insert the article into the 'articles' collection
      article => articleService.save(article).map(_ => Redirect(routes.ArticlesController.index))
    )
  }

  def edit(id: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    import reactivemongo.bson.BSONDateTime

    Article.form.bindFromRequest.fold(
      errors => Future.successful(
        Ok(views.html.editArticle(Some(request.identity), Some(id), errors, None))),

      articleFromForm => {
        // create a modifier document, ie a document that contains the update operations to run onto the documents matching the query
        val modifier = Article(
            id = articleFromForm.id,
            updateDate = Some(new DateTime()),
            title = articleFromForm.title,
            content = articleFromForm.content,
            publisher = articleFromForm.publisher)

        // ok, let's do the update
        articleService.save(modifier)
          .map { _ => Redirect(routes.ArticlesController.index) }
      })
  }

  def delete(id: String) = silhouette.SecuredAction(AlwaysAuthorized()).async {
    // let's collect all the attachments matching that match the article to delete
    (for {
      fs <- gridFS
      files <- fs.find[JsObject, JSONReadFile](Json.obj("article" -> id)).collect[List]()
      _ <- {
        // for each attachment, delete their chunks and then their file entry
        def deletions = files.map(fs.remove(_))

        Future.sequence(deletions)
      } 
      _ <- {
        // now, the last operation: remove the article
        articleService.remove(Article(id = Some(id)))
      }
    } yield Ok).recover { case _ => InternalServerError }
  }

  // save the uploaded file as an attachment of the article with the given id
  def saveAttachment(id: String) = {
    def fs = Await.result(gridFS, Duration("5s"))
    silhouette.SecuredAction(AlwaysAuthorized()).async(gridFSBodyParser(fs)) { request =>
      // here is the future file!
      val futureFile = request.body.files.head.ref

      futureFile.onFailure {
        case err => err.printStackTrace()
      }

      // when the upload is complete, we add the article id to the file entry (in order to find the attachments of the article)
      val futureUpdate = for {
        file <- futureFile
        // here, the file is completely uploaded, so it is time to update the article
        updateResult <- fs.files.update(
          Json.obj("_id" -> file.id),
          Json.obj("$set" -> Json.obj("article" -> id)))
      } yield Redirect(routes.ArticlesController.showEditForm(id))

      futureUpdate.recover {
        case e => InternalServerError(e.getMessage())
      }
    }
  }

  def getAttachment(id: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { request =>
    gridFS.flatMap { fs =>
      // find the matching attachment, if any, and streams it to the client
      val file = fs.find[JsObject, JSONReadFile](Json.obj("_id" -> id))

      request.getQueryString("inline") match {
        case Some("true") =>
          serve[JsString, JSONReadFile](fs)(file, CONTENT_DISPOSITION_INLINE)

        case _ => serve[JsString, JSONReadFile](fs)(file)
      }
    }
  }

  def removeAttachment(id: String) = silhouette.SecuredAction(AlwaysAuthorized()).async {
    gridFS.flatMap(_.remove(Json toJson id).map(_ => Ok).
      recover { case _ => InternalServerError })
  }

  private def getSort(request: Request[_]): Option[(ModelKey, Boolean)] =
    request.queryString.get("sort").map { fields =>
      val sortBy = for {
        order <- fields.map { field =>
          if (field.startsWith("-"))
            field.drop(1) -> -1
          else field -> 1
        }
        if order._1 == "title" || order._1 == "publisher" || order._1 == "creationDate" || order._1 == "updateDate"
      } yield keyToArticleKey(order._1) -> (order._2 match { 
        case -1 => false
        case default => true
        })

      sortBy.head
    }
  
  private def keyToArticleKey(key: String): ModelKey = key match {
    case "title" => Article.Keys.Title
    case "publisher" => Article.Keys.Publisher
    case "creationDate" => Article.Keys.CreationDate
    case "updateDate" => Article.Keys.UpdateDate
    case default => Article.Keys.Title
  }

}