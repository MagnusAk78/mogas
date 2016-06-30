package models

import org.joda.time.DateTime

import play.api.data._
import play.api.data.Forms.{ text, longNumber, mapping, nonEmptyText, optional }
import play.api.data.validation.Constraints.pattern

import reactivemongo.bson.{
  BSONDateTime, BSONDocument, BSONObjectID
}

case class Article(
  id: Option[String] = None,
  title: Option[String] = None,
  content: Option[String] = None,
  publisher: Option[String] = None,
  creationDate: Option[DateTime] = None,
  updateDate: Option[DateTime] = None) extends BaseModel

// Turn off your mind, relax, and float downstream
// It is not dying...
object Article {
  import play.api.libs.json._
  
  object Keys {
    case object Id extends ModelKey("_id")
    case object Title extends ModelKey("title")
    case object Content extends ModelKey("content")
    case object Publisher extends ModelKey("publisher")
    case object CreationDate extends ModelKey("creationDate")
    case object UpdateDate extends ModelKey("updateDate")
  }

  implicit object ArticleWrites extends OWrites[Article] {
    
    type JsField = (String, JsValue)
    
      def writes(article: Article): JsObject = {
        val sequence = Seq[JsField]() ++
          article.id.map(Keys.Id.value -> JsString(_)) ++
          article.title.map(Keys.Title.value -> Json.toJson(_)) ++
          article.content.map(Keys.Content.value -> JsString(_)) ++
          article.publisher.map(Keys.Publisher.value -> JsString(_)) ++
          article.creationDate.map(Keys.CreationDate.value -> Json.toJson(_)) ++
          article.updateDate.map(Keys.UpdateDate.value -> Json.toJson(_))
          
        JsObject(sequence)
      }    
  }

  implicit object ArticleReads extends Reads[Article] {
    def reads(json: JsValue): JsResult[Article] = json match {
      case obj: JsObject => try {
        val id = (obj \ Keys.Id.value).asOpt[String]
        val title = (obj \ Keys.Title.value).asOpt[String]
        val content = (obj \ Keys.Content.value).asOpt[String]
        val publisher = (obj \ Keys.Publisher.value).asOpt[String]
        val creationDate = (obj \ Keys.CreationDate.value).asOpt[Long]
        val updateDate = (obj \ Keys.UpdateDate.value).asOpt[Long]

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