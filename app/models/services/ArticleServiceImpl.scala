package models.services

import scala.concurrent.Future
import models.daos.ArticleDAO
import models.Article
import javax.inject.Inject

class ArticleServiceImpl @Inject()(override val dao: ArticleDAO) extends ArticleService {

}