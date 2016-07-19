package controllers.actions

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import javax.inject.Singleton
import models.services.OrganisationService
import play.api.mvc.ActionRefiner
import utils.auth.DefaultEnv
import scala.concurrent.ExecutionContext
import play.api.mvc.Results.NotFound
import models.BaseModel
import models.services.UserService
import models.Organisation
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionTransformer
import play.api.mvc.ActionFunction
import controllers.AlwaysAuthorized

trait GeneralActions {
  
  def MySecuredAction: ActionBuilder[MySecuredRequest]
  
  def MyUserAwareAction: ActionBuilder[MyUserAwareRequest]
  
  def UserAction(uuid: String): ActionRefiner[MySecuredRequest, UserRequest]
  
  def OrganisationAction(uuid: String): ActionRefiner[MySecuredRequest, OrganisationRequest]
}

@Singleton
class GeneralActionsImpl @Inject() (
  val organisationService: OrganisationService,
  val userService: UserService,
  val silhouette: Silhouette[DefaultEnv])
  (implicit ec: ExecutionContext) extends GeneralActions { 
    
  def ActiveOrganisationAction = new ActionTransformer[SilhouetteSecuredRequest, MySecuredRequest] {
    override def transform[A](request: SilhouetteSecuredRequest[A]) = {
      organisationService.findOneByUuid(request.identity.activeOrganisation).map { optActiveOrg =>  
        MySecuredRequest(optActiveOrg, request)
        }
      }
  }
  
  def MyUserAwareTransformer = new ActionTransformer[SilhouetteUserAwareRequest, MyUserAwareRequest] {
    override def transform[A](request: SilhouetteUserAwareRequest[A]) = Future.successful(MyUserAwareRequest(request))
  }
  
  override def MySecuredAction = silhouette.SecuredAction(AlwaysAuthorized()) andThen ActiveOrganisationAction
  
  override def MyUserAwareAction = silhouette.UserAwareAction andThen MyUserAwareTransformer
    
  override def UserAction(uuid: String) = new ActionRefiner[MySecuredRequest, UserRequest] {
    override def refine[A](activeOrganisationRequest: MySecuredRequest[A]) = {
      userService.findOneByUuid(uuid).map {optUser => optUser.map(
          UserRequest(_, activeOrganisationRequest)).toRight(NotFound)
      }
    }
  }  
  
  override def OrganisationAction(uuid: String) = new ActionRefiner[MySecuredRequest, OrganisationRequest] {
    override def refine[A](activeOrganisationRequest: MySecuredRequest[A]) = {
        for {
          optionOrganisation <- organisationService.findOneByUuid(uuid)
        } yield optionOrganisation.map(organisation => OrganisationRequest(organisation, activeOrganisationRequest)).toRight(NotFound)
    }
  }  
}