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
import models.services.InstructionService
import models.User
import models.Factory
import models.Interface
import models.Hierarchy
import models.Element
import models.Instruction
import models.InstructionPart
import models.services.AmlObjectService
import models.services.IssueService
import models.Issue
import models.IssueUpdate

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

  def AmlObjectAction(uuid: String): ActionRefiner[MySecuredRequest, AmlObjectRequest]

  def InstructionAction(uuid: String): ActionRefiner[MySecuredRequest, InstructionRequest]

  def InstructionPartAction(uuid: String): ActionRefiner[MySecuredRequest, InstructionPartRequest]

  def IssueAction(uuid: String): ActionRefiner[MySecuredRequest, IssueRequest]

  def IssueUpdateAction(uuid: String): ActionRefiner[MySecuredRequest, IssueUpdateRequest]
}

@Singleton
class GeneralActionsImpl @Inject() (
    val organisationService: OrganisationService,
    val userService: UserService,
    val factoryService: FactoryService,
    val amlObjectService: AmlObjectService,
    val instructionService: InstructionService,
    val issueService: IssueService,
    val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext) extends GeneralActions {

  def ActiveOrganisationAction = new ActionTransformer[SilhouetteSecuredRequest, MySecuredRequest] {
    override def transform[A](request: SilhouetteSecuredRequest[A]) = {
      organisationService.findOne(Organisation.queryByUuid(request.identity.activeOrganisation)).map { optActiveOrg =>
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
      userService.findOne(User.queryByUuid(uuid)).map { optUser =>
        optUser.map(
          UserRequest(_, activeOrganisationRequest)).toRight(NotFound)
      }
    }
  }

  override def OrganisationAction(uuid: String) = new ActionRefiner[MySecuredRequest, OrganisationRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionOrganisation <- organisationService.findOne(Organisation.queryByUuid(uuid))
      } yield optionOrganisation.map(organisation => OrganisationRequest(organisation, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def FactoryAction(uuid: String) = new ActionRefiner[MySecuredRequest, FactoryRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionFactory <- factoryService.findOneFactory(Factory.queryByUuid(uuid))
      } yield optionFactory.map(factory => FactoryRequest(factory, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def HierarchyAction(uuid: String) = new ActionRefiner[MySecuredRequest, HierarchyRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionHierarchy <- factoryService.findOneHierarchy(Factory.queryByUuid(uuid))
        optionFactory <- factoryService.findOneFactory(Factory.queryByUuid(optionHierarchy.get.parent))
      } yield optionFactory.map(factory => HierarchyRequest(optionFactory.get, optionHierarchy.get, mySecuredRequest))
        .toRight(NotFound)
    }
  }

  override def ElementAction(uuid: String) = new ActionRefiner[MySecuredRequest, ElementRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        elements <- amlObjectService.getElementChain(uuid)
        optionHierarchy <- factoryService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optionFactory <- factoryService.findOneFactory(Factory.queryByUuid(optionHierarchy.get.parent))
      } yield optionFactory.map(factory => ElementRequest(optionFactory.get, optionHierarchy.get, elements,
        mySecuredRequest)).toRight(NotFound)
    }
  }

  override def InterfaceAction(uuid: String) = new ActionRefiner[MySecuredRequest, InterfaceRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        interface <- amlObjectService.findOneInterface(Interface.queryByUuid(uuid))
        elements <- amlObjectService.getElementChain(interface.get.parent)
        optionHierarchy <- factoryService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optionFactory <- factoryService.findOneFactory(Factory.queryByUuid(optionHierarchy.get.parent))
      } yield optionFactory.map(factory => InterfaceRequest(optionFactory.get, optionHierarchy.get, elements,
        interface.get, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def AmlObjectAction(uuid: String) = new ActionRefiner[MySecuredRequest, AmlObjectRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        interface <- amlObjectService.findOneInterface(Interface.queryByUuid(uuid))
        element <- interface.map(i => Future.successful(None)).getOrElse(amlObjectService.findOneElement(Element.queryByUuid(uuid)))
      } yield interface match {
        case Some(i) => Right(AmlObjectRequest(Right(i), mySecuredRequest))
        case None => element.map(e => AmlObjectRequest(Left(e), mySecuredRequest)).toRight(NotFound)
      }
    }
  }

  override def InstructionAction(uuid: String) = new ActionRefiner[MySecuredRequest, InstructionRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionInstruction <- instructionService.findOneInstruction(Instruction.queryByUuid(uuid))
      } yield optionInstruction.map(i => InstructionRequest(i, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def InstructionPartAction(uuid: String) = new ActionRefiner[MySecuredRequest, InstructionPartRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionInstructionPart <- instructionService.findOneInstructionPart(InstructionPart.queryByUuid(uuid))
        optionInstruction <- instructionService.findOneInstruction(Instruction.queryByUuid(optionInstructionPart.get.parent))
      } yield (optionInstructionPart, optionInstruction) match {
        case (Some(instructionPart), Some(instruction)) => Right(InstructionPartRequest(instructionPart, instruction, mySecuredRequest))
        case _ => Left(NotFound)
      }
    }
  }

  def IssueAction(uuid: String): ActionRefiner[MySecuredRequest, IssueRequest] = new ActionRefiner[MySecuredRequest, IssueRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionIssue <- issueService.findOneIssue(Issue.queryByUuid(uuid))
      } yield optionIssue.map(i => IssueRequest(i, mySecuredRequest)).toRight(NotFound)
    }
  }

  def IssueUpdateAction(uuid: String): ActionRefiner[MySecuredRequest, IssueUpdateRequest] = new ActionRefiner[MySecuredRequest, IssueUpdateRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionIssueUpdate <- issueService.findOneIssueUpdate(IssueUpdate.queryByUuid(uuid))
        optionIssue <- issueService.findOneIssue(Issue.queryByUuid(optionIssueUpdate.get.parent))
      } yield (optionIssueUpdate, optionIssue) match {
        case (Some(issueUpdate), Some(issue)) => Right(IssueUpdateRequest(issueUpdate, issue, mySecuredRequest))
        case _ => Left(NotFound)
      }
    }
  }
}