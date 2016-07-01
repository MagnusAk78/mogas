package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models._
import play.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Controller

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import models.daos.UserDAO
import com.sun.corba.se.spi.ior.ObjectId
import utils.auth.DefaultEnv
import models.daos.OrganisationDAO
import models.daos.UserDAO
import models.daos.OrganisationDAO
import models.services.OrganisationService
import models.daos.UserDAO
import models.daos.OrganisationDAO
import models.daos.UserDAO
import models.daos.OrganisationDAO
import models.daos.UserDAO
import models.daos.OrganisationDAO
import models.daos.UserDAO
import forms.OrganisationForm
import models.daos.OrganisationDAO

class OrganisationController @Inject()(val messagesApi: MessagesApi, val silhouette: Silhouette[DefaultEnv],
    val organisationService: OrganisationService)(implicit exec: ExecutionContext)
  extends Controller with I18nSupport {

  def list(organisationsPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>

    val selector = Organisation(allowedUsers = request.identity.id.toSet)
    
    val responses = for {
      futureCount <- organisationService.count(selector)
      futureOrgList <- organisationService.find(selector, organisationsPage, utils.DefaultValues.DefaultPageLength)
    } yield (futureOrgList, futureCount)
    
    responses.map( resultTuple =>
        Ok(views.html.organisations.list(resultTuple._1, resultTuple._2, organisationsPage, utils.DefaultValues.DefaultPageLength, 
            Some(request.identity)))
            )       
  }
  

  def save = silhouette.SecuredAction(AlwaysAuthorized()).async(parse.multipartFormData) { implicit request =>
    Logger.info("OrganisationController.save")

    //Get the id from the form data and see if it exists in the database
    val futureOpOrg: Future[Option[Organisation]] = request.body.asFormUrlEncoded.get("id") match {
      case Some(idString :: ignoringTheTail) => organisationService.find(Organisation(id = Some(idString)), maxDocs = 1).map(_.headOption)
      case _ => Future(None)
    }

    OrganisationForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(
          BadRequest(views.html.organisations.edit(formWithErrors, true, Some(request.identity)))
        ),
      formData => {

        //val imageFileReps = Organisation.saveImageFile(request.body.file(models.imageFileKeyString))

        val newOrg = Organisation(
          id = formData.id,
          name = formData.name,
          allowedUsers = organisationOpt map(_.allowedUsers) getOrElse (Set(request.identity._id)) /*,
          imageFileRep = imageFileReps._1.orElse(organisationOpt.flatMap(_.imageFileRep)),
          thumbnailFileRep = imageFileReps._2.orElse(organisationOpt.flatMap(_.thumbnailFileRep)) */)

        if(organisationOpt.isDefined) {
          OrganisationDAO.update(OrganisationParams(_id = Some(newOrg._id)), newOrg)
        } else {
          OrganisationDAO.insert(newOrg)
        }

        Future.successful(Redirect(routes.OrganisationController.edit(newOrg._id.toString)).
          flashing("success" -> Messages("db.success.update", newOrg.name)))
      }
    )
  }

  def edit(idString: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.edit")

    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) => {
        Future.successful(Ok(views.html.organisations.edit(Organisation.organisationForm(Some(organisation)).
          fill(organisation), true, Some(request.identity))))
      }
      case None =>
        val orgForm = Organisation.organisationForm(None).fill(Organisation(new ObjectId, "",
          Set(request.identity._id), None, None))
        Future.successful(Ok(views.html.organisations.edit(orgForm, false, Some(request.identity))))
    }
  }

  def editActiveOrganisation(page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editActivatedOrganisation")

    val orgCursor = OrganisationDAO.find(OrganisationParams(allowedUsers = Some(request.identity._id)))
    val count = orgCursor.count
    val organisationList = DbHelper.paginate(orgCursor, page, models.defaultPageLength).toList

    Future.successful(Ok(views.html.organisations.editActivateOrganisation(organisationList, count,
      page, models.defaultPageLength, Some(request.identity))))
  }

  def setActiveOrganisation(idString: String, page: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) =>
        val newUser = if (request.identity.activeOrganisation.isDefined &&
          request.identity.activeOrganisation.get == idString) {
          request.identity.copy(activeOrganisation = None)
        } else {
          request.identity.copy(activeOrganisation = Some(idString))
        }
        UserDAO.update(UserParams(_id = Some(newUser._id)), newUser)

        val orgCursor = OrganisationDAO.find(OrganisationParams(allowedUsers = Some(request.identity._id)))
        val count = orgCursor.count
        val organisationList = DbHelper.paginate(orgCursor, page, models.defaultPageLength).toList

        Future.successful(Ok(views.html.organisations.editActivateOrganisation(organisationList, count,
          page, models.defaultPageLength, Some(newUser))))
      case None =>
        Future.successful(Redirect(routes.OrganisationController.list(1)).
          flashing("error" -> Messages("db.error.read")))
    }
  }

  def editAllowedUsers(idString: String, usersPage: Int) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.editAllowedUsers")

    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) => {
        val usersCursor = UserDAO.find(DbHelper.queryAll)
        val usersCount = usersCursor.count
        val users = DbHelper.paginate(usersCursor, usersPage, models.defaultPageLength).toList

        Future.successful(Ok(views.html.organisations.editAllowedUsers(organisation, users,
          usersCount, usersPage, models.defaultPageLength, Some(request.identity))))
      }
      case None =>
        Future.successful(Redirect(routes.OrganisationController.list(1)).
          flashing("error" -> Messages("db.error.read")))
    }
  }

  def changeAllowedUser(idString: String, userIdString: String, usersPage: Int) =
    silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
      Logger.info("OrganisationController.changeAllowedUser")

      OrganisationDAO.findOneById(idString) match {
        case Some(organisation) => {
          if (organisation.allowedUsers.contains(request.identity._id)) {
            //Access allowed

            val userObjectId = new ObjectId(userIdString)
            val newOrganisation = organisation.allowedUsers.contains(userObjectId) match {
              case true => organisation.copy(allowedUsers = organisation.allowedUsers - userObjectId)
              case false => organisation.copy(allowedUsers = organisation.allowedUsers + userObjectId)
            }

            if (newOrganisation.allowedUsers.isEmpty) {
              //This is not ok, some user must be allowed to see/change it
              Future.successful(Redirect(routes.OrganisationController.editAllowedUsers(organisation._id.toString,
                usersPage)).flashing("error" -> Messages("organisation.minimum.one.user")))
            } else if (newOrganisation.allowedUsers.contains(request.identity._id) == false) {
              //This is not ok, the logged in user must be part of the organisation.
              Future.successful(Redirect(routes.OrganisationController.editAllowedUsers(organisation._id.toString,
                usersPage)).flashing("error" -> Messages("organisation.remove.self.not.allowed")))
            } else {
              OrganisationDAO.update(OrganisationParams(_id = Some(newOrganisation._id)), newOrganisation)

              Future.successful(Redirect(routes.OrganisationController.editAllowedUsers(organisation._id.toString,
                usersPage)).flashing("success" -> Messages("db.success.update", organisation.name)))
            }
          } else {
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("access.denied")))
          }
        }
        case None =>
          val orgForm = Organisation.organisationForm(None).fill(Organisation(new ObjectId, "",
            Set(request.identity._id), None, None))
          Future.successful(Redirect(routes.OrganisationController.list(1)).
            flashing("error" -> Messages("db.error.read")))
      }
    }

  def delete(idString: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    Logger.info("OrganisationController.delete idString: " + idString)

    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) => {
        //The user can't delete if not in the allowed users list
        if (organisation.allowedUsers.contains(request.identity._id)) {
          //We can't remove organisations that have dependencies to factories
          val depFacCursor = FactoryDAO.find(FactoryParams(organisation = Some(organisation._id))).
            sort(DbHelper.sortAscKey(Factory.nameKey))

          if(depFacCursor.isEmpty) {
            OrganisationDAO.remove(organisation)
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("success" -> Messages("db.success.remove", organisation.name)))
          } else {
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.dependencies", "Factory: " + depFacCursor.next.name)))
          }
        } else {
          Future.successful(Redirect(routes.OrganisationController.list(1)).
            flashing("error" -> Messages("access.denied")))
        }
      }
      case None => {
        Future.successful(Redirect(routes.OrganisationController.list(1)).
          flashing("error" -> Messages("db.error.find", "Organisation", idString)))
      }
    }
  }

  def image(idString: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) => {
        Organisation.getImageFile(organisation) match {
          case Some(imageFile) => {
            Future.successful(Ok.sendFile(content = imageFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.read.file", idString)))
        }
      }
      case None => Future.successful(Redirect(routes.OrganisationController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }

  def thumbnail(idString: String) = silhouette.SecuredAction(AlwaysAuthorized()).async { implicit request =>
    OrganisationDAO.findOneById(idString) match {
      case Some(organisation) => {
        Organisation.getThumbnailFile(organisation) match {
          case Some(thumbnailFile) => {
            Future.successful(Ok.sendFile(content = thumbnailFile, inline = true))
          }
          case None =>
            Future.successful(Redirect(routes.OrganisationController.list(1)).
              flashing("error" -> Messages("db.error.read.file", idString)))
        }
      }
      case None => Future.successful(Redirect(routes.OrganisationController.list(1)).
        flashing("error" -> Messages("db.read.error")))
    }
  }
}
