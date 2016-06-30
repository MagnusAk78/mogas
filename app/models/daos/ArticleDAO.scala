package models.daos

import scala.concurrent.Future
import models.Article
import models.ArticleKeys

trait ArticleDAO {
  
  def save(article: Article): Future[Option[Article]]
  
  def remove(article: Article): Future[Boolean]
  
  def find(article: Article, maxDocs: Int = 0): Future[List[Article]]
  
  def findAndSort(article: Article, sortBy: ArticleKeys.Value, ascending: Boolean, maxDocs: Int = 0): Future[List[Article]]
}