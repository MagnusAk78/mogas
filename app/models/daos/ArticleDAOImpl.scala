package models.daos

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import javax.inject.Inject
import models.Article
import models.ArticleKeys
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.collection.JSONCollectionProducer

class ArticleDAOImpl @Inject() (val reactiveMongoApi: ReactiveMongoApi)(implicit exec: ExecutionContext) extends ArticleDAO {

  private def collection = reactiveMongoApi.database.map(_.collection[JSONCollection]("articles"))
  
  override def save(article: Article): Future[Option[Article]] = article.id match {
    case Some(id) => {
      //This has an id, it is probably an update
      val selector = Article(id = Some(id))

      collection.flatMap(_.update(selector = selector, article).map { wr =>
        wr.ok match {
          case true  => Some(article)
          case false => None
        }
      })
    }
    case None => {
      //This has no id, it is an insert
      collection.flatMap(_.insert(article).map { wr =>
        wr.ok match {
          case true  => Some(article)
          case false => None
        }
      })
    }
  }

  override def remove(article: Article): Future[Boolean] = collection.flatMap(_.remove(article).map { wr => wr.ok })

  override def find(article: Article, maxDocs: Int = 0): Future[List[Article]] =
    collection.flatMap(_.find(article).cursor[Article]().collect[List](maxDocs))

  override def findAndSort(article: Article, sortBy: ArticleKeys.Value, ascending: Boolean, maxDocs: Int = 0): Future[List[Article]] =
    collection.flatMap(_.find(article).sort(DaoHelper.getSortByJsObject(sortBy.toString, ascending)).cursor[Article]().collect[List](maxDocs))
}