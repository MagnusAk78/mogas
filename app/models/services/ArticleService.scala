package models.services

import scala.concurrent.Future
import models.Article
import models.daos.ArticleDAO

trait ArticleService extends BaseModelService[Article, ArticleDAO] {

}