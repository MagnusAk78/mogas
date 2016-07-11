package controllers

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.Logger
import play.api.mvc.Request

import scala.concurrent.Future

case class AlwaysAuthorized() extends Authorization[User, CookieAuthenticator] {
  
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(
    implicit request: Request[B]) = {
    
    Logger.info("isAuthorized is running, I reutrn true!!!")
    
    Future.successful(true)
  }
}

