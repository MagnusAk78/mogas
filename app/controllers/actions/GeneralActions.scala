package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Silhouette

import controllers.AlwaysAuthorized
import javax.inject.Inject
import javax.inject.Singleton
import models.Organisation
import models.services.FactoryService
import models.services.OrganisationService
import models.services.UserService
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionFilter
import play.api.mvc.ActionFunction
import play.api.mvc.ActionRefiner
import play.api.mvc.ActionTransformer
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import utils.auth.DefaultEnv
import models.services.HierarchyService
import models.services.InternalElementService
import models.services.ExternalInterfaceService

trait GeneralActions {

  def MySecuredAction: ActionBuilder[MySecuredRequest]

  def RequireActiveOrganisation: ActionFilter[MySecuredRequest]

  def MyUserAwareAction: ActionBuilder[MyUserAwareRequest]

  def UserAction(uuid: String): ActionRefiner[MySecuredRequest, UserRequest]

  def OrganisationAction(uuid: String): ActionRefiner[MySecuredRequest, OrganisationRequest]

  def FactoryAction(uuid: String): ActionRefiner[MySecuredRequest, FactoryRequest]

  def HierarchyAction(uuid: String): ActionRefiner[MySecuredRequest, HierarchyRequest]

  def ElementAction(uuid: String): ActionRefiner[MySecuredRequest, ElementRequest]

  def InterfaceAction(uuid: String): ActionRefiner[MySecuredRequest, InterfaceRequest]
}

@Singleton
class GeneralActionsImpl @Inject() (
    val organisationService: OrganisationService,
    val userService: UserService,
    val factoryService: FactoryService,
    val hierarchyService: HierarchyService,
    val internalElementService: InternalElementService,
    val externalInterfaceService: ExternalInterfaceService,
    val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext) extends GeneralActions {

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

  override def RequireActiveOrganisation: ActionFilter[MySecuredRequest] = new ActionFilter[MySecuredRequest] {
    override def filter[A](mySecuredRequest: MySecuredRequest[A]): Future[Option[Result]] =
      Future.successful(mySecuredRequest.activeOrganisation.map(activeOrg => None).getOrElse(Some(NotFound)))
  }

  override def UserAction(uuid: String) = new ActionRefiner[MySecuredRequest, UserRequest] {
    override def refine[A](activeOrganisationRequest: MySecuredRequest[A]) = {
      userService.findOneByUuid(uuid).map { optUser =>
        optUser.map(
          UserRequest(_, activeOrganisationRequest)).toRight(NotFound)
      }
    }
  }

  override def OrganisationAction(uuid: String) = new ActionRefiner[MySecuredRequest, OrganisationRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionOrganisation <- organisationService.findOneByUuid(uuid)
      } yield optionOrganisation.map(organisation => OrganisationRequest(organisation, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def FactoryAction(uuid: String) = new ActionRefiner[MySecuredRequest, FactoryRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionFactory <- factoryService.findOneByUuid(uuid)
      } yield optionFactory.map(factory => FactoryRequest(factory, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def HierarchyAction(uuid: String) = new ActionRefiner[MySecuredRequest, HierarchyRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionHierarchy <- hierarchyService.findOneByUuid(uuid)
        optionFactory <- factoryService.findOneByUuid(optionHierarchy.get.factory)
      } yield optionFactory.map(factory => HierarchyRequest(optionFactory.get, optionHierarchy.get, mySecuredRequest))
        .toRight(NotFound)
    }
  }

  override def ElementAction(uuid: String) = new ActionRefiner[MySecuredRequest, ElementRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        elements <- internalElementService.getElementChain(uuid)
        optionHierarchy <- hierarchyService.findOneByUuid(elements.head.parent)
        optionFactory <- factoryService.findOneByUuid(optionHierarchy.get.factory)
      } yield optionFactory.map(factory => ElementRequest(optionFactory.get, optionHierarchy.get, elements,
        mySecuredRequest)).toRight(NotFound)
    }
  }

  override def InterfaceAction(uuid: String) = new ActionRefiner[MySecuredRequest, InterfaceRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        interface <- externalInterfaceService.findOneByUuid(uuid)
        elements <- internalElementService.getElementChain(interface.get.parent)
        optionHierarchy <- hierarchyService.findOneByUuid(elements.head.parent)
        optionFactory <- factoryService.findOneByUuid(optionHierarchy.get.factory)
      } yield optionFactory.map(factory => InterfaceRequest(optionFactory.get, optionHierarchy.get, elements,
        interface.get, mySecuredRequest)).toRight(NotFound)
    }
  }
}