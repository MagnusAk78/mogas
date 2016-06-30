package models.services

import scala.concurrent.Future
import models.daos.ArticleDAO
import models.Article
import javax.inject.Inject
import models.ArticleKeys

class ArticleServiceImpl @Inject()(val articleDAO: ArticleDAO) extends ArticleService {
  
  override def save(article: Article): Future[Option[Article]] = articleDAO.save(article)
  
  override def remove(article: Article): Future[Boolean] = articleDAO.remove(article)
  
  override def find(article: Article, maxDocs: Int = 0): Future[List[Article]] = articleDAO.find(article, maxDocs)
  
  override def findAndSort(article: Article, sortBy: ArticleKeys.Value, ascending: Boolean, maxDocs: Int = 0): Future[List[Article]] =
    articleDAO.findAndSort(article, sortBy, ascending, maxDocs)
}