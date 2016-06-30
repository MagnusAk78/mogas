package models

import org.joda.time.DateTime

import play.api.data._
import play.api.data.Forms.{ text, longNumber, mapping, nonEmptyText, optional }
import play.api.data.validation.Constraints.pattern

import reactivemongo.bson.{
  BSONDateTime, BSONDocument, BSONObjectID
}

object ArticleKeys extends Enumeration {
   val id = Value("_id")
   val title = Value("title")
   val content = Value("content")
   val publisher = Value("publisher")
   val creationDate = Value("creationDate")
   val updateDate = Value("updateDate")
} 

case class Article(
  id: Option[String] = None,
  title: Option[String] = None,
  content: Option[String] = None,
  publisher: Option[String] = None,
  creationDate: Option[DateTime] = None,
  updateDate: Option[DateTime] = None)

// Turn off your mind, relax, and float downstream
// It is not dying...
object Article {
  import play.api.libs.json._

  implicit object ArticleWrites extends OWrites[Article] {
    
    type JsField = (String, JsValue)
    
      def writes(article: Article): JsObject = {
        val sequence = Seq[JsField]() ++
          article.id.map(ArticleKeys.id.toString -> JsString(_)) ++
          article.title.map(ArticleKeys.title.toString -> Json.toJson(_)) ++
          article.content.map(ArticleKeys.content.toString -> JsString(_)) ++
          article.publisher.map(ArticleKeys.publisher.toString -> JsString(_)) ++
          article.creationDate.map(ArticleKeys.creationDate.toString -> Json.toJson(_)) ++
          article.updateDate.map(ArticleKeys.updateDate.toString -> Json.toJson(_))
          
        JsObject(sequence)
      }    
  }

  implicit object ArticleReads extends Reads[Article] {
    def reads(json: JsValue): JsResult[Article] = json match {
      case obj: JsObject => try {
        val id = (obj \ ArticleKeys.id.toString).asOpt[String]
        val title = (obj \ ArticleKeys.title.toString).asOpt[String]
        val content = (obj \ ArticleKeys.content.toString).asOpt[String]
        val publisher = (obj \ ArticleKeys.publisher.toString).asOpt[String]
        val creationDate = (obj \ ArticleKeys.creationDate.toString).asOpt[Long]
        val updateDate = (obj \ ArticleKeys.updateDate.toString).asOpt[Long]

        JsSuccess(Article(id, title, content, publisher,
          creationDate.map(new DateTime(_)),
          updateDate.map(new DateTime(_))))
        
      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }

  val form = Form(
    mapping(
      "id" -> optional(text verifying pattern(
        """[a-fA-F0-9]{24}""".r, error = "error.objectId")),
      "title" -> optional(nonEmptyText),
      "content" -> optional(text),
      "publisher" -> optional(nonEmptyText),
      "creationDate" -> optional(longNumber),
      "updateDate" -> optional(longNumber)) {
      (id, title, content, publisher, creationDate, updateDate) =>
      Article(
        id,
        title,
        content,
        publisher,
        creationDate.map(new DateTime(_)),
        updateDate.map(new DateTime(_)))
    } { article =>
      Some(
        (article.id,
          article.title,
          article.content,
          article.publisher,
          article.creationDate.map(_.getMillis),
          article.updateDate.map(_.getMillis)))
    })
}