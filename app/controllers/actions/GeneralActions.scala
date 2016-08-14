package controllers.actions

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.mohiva.play.silhouette.api.Silhouette

import controllers.AlwaysAuthorized
import javax.inject.Inject
import javax.inject.Singleton
import models.services.DomainService
import models.services.DomainService
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
import models.Domain
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

  def RequireActiveDomain: ActionFilter[MySecuredRequest]

  def MyUserAwareAction: ActionBuilder[MyUserAwareRequest]

  def UserAction(uuid: String): ActionRefiner[MySecuredRequest, UserRequest]

  def DomainAction(uuid: String): ActionRefiner[MySecuredRequest, DomainRequest]

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
    val domainService: DomainService,
    val userService: UserService,
    val amlObjectService: AmlObjectService,
    val instructionService: InstructionService,
    val issueService: IssueService,
    val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext) extends GeneralActions {

  def ActiveDomainAction = new ActionTransformer[SilhouetteSecuredRequest, MySecuredRequest] {
    override def transform[A](request: SilhouetteSecuredRequest[A]) = {
      domainService.findOneDomain(Domain.queryByUuid(request.identity.activeDomain)).map { optActiveOrg =>
        MySecuredRequest(optActiveOrg, request)
      }
    }
  }

  def MyUserAwareTransformer = new ActionTransformer[SilhouetteUserAwareRequest, MyUserAwareRequest] {
    override def transform[A](request: SilhouetteUserAwareRequest[A]) = Future.successful(MyUserAwareRequest(request))
  }

  override def MySecuredAction = silhouette.SecuredAction(AlwaysAuthorized()) andThen ActiveDomainAction

  override def MyUserAwareAction = silhouette.UserAwareAction andThen MyUserAwareTransformer

  override def RequireActiveDomain: ActionFilter[MySecuredRequest] = new ActionFilter[MySecuredRequest] {
    override def filter[A](mySecuredRequest: MySecuredRequest[A]): Future[Option[Result]] =
      Future.successful(mySecuredRequest.activeDomain.map(activeDomain => None).getOrElse(Some(NotFound)))
  }

  override def UserAction(uuid: String) = new ActionRefiner[MySecuredRequest, UserRequest] {
    override def refine[A](activeDomainRequest: MySecuredRequest[A]) = {
      userService.findOne(User.queryByUuid(uuid)).map { optUser =>
        optUser.map(
          UserRequest(_, activeDomainRequest)).toRight(NotFound)
      }
    }
  }

  override def DomainAction(uuid: String) = new ActionRefiner[MySecuredRequest, DomainRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionDomain <- domainService.findOneDomain(Domain.queryByUuid(uuid))
      } yield optionDomain.map(domain => DomainRequest(domain, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def HierarchyAction(uuid: String) = new ActionRefiner[MySecuredRequest, HierarchyRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionHierarchy <- domainService.findOneHierarchy(Domain.queryByUuid(uuid))
        optionDomain <- domainService.findOneDomain(Domain.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => HierarchyRequest(optionDomain.get, optionHierarchy.get, mySecuredRequest))
        .toRight(NotFound)
    }
  }

  override def ElementAction(uuid: String) = new ActionRefiner[MySecuredRequest, ElementRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        elements <- amlObjectService.getElementChain(uuid)
        optionHierarchy <- domainService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optionDomain <- domainService.findOneDomain(Domain.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => ElementRequest(optionDomain.get, optionHierarchy.get, elements,
        mySecuredRequest)).toRight(NotFound)
    }
  }

  override def InterfaceAction(uuid: String) = new ActionRefiner[MySecuredRequest, InterfaceRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        interface <- amlObjectService.findOneInterface(Interface.queryByUuid(uuid))
        elements <- amlObjectService.getElementChain(interface.get.parent)
        optionHierarchy <- domainService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optionDomain <- domainService.findOneDomain(Domain.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => InterfaceRequest(optionDomain.get, optionHierarchy.get, elements,
        interface.get, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def AmlObjectAction(uuid: String) = new ActionRefiner[MySecuredRequest, AmlObjectRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optInterface <- amlObjectService.findOneInterface(Interface.queryByUuid(uuid))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(Element.queryByUuid(uuid))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(Domain.queryByUuid(optHierarchy.get.parent))
      } yield if (optHierarchy.isDefined && optDomain.isDefined) {
        Right(AmlObjectRequest(optDomain.get, optHierarchy.get, elements, optInterface, mySecuredRequest))
      } else {
        Left(NotFound)
      }
    }
  }

  override def InstructionAction(uuid: String) = new ActionRefiner[MySecuredRequest, InstructionRequest] {
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optInstruction <- instructionService.findOneInstruction(Instruction.queryByUuid(uuid))
        optInterface <- amlObjectService.findOneInterface(Interface.queryByUuid(optInstruction.get.parent))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(Element.queryByUuid(optInstruction.get.parent))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(Hierarchy.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(Domain.queryByUuid(optHierarchy.get.parent))
      } yield if (optInstruction.isDefined && optHierarchy.isDefined && optDomain.isDefined) {
        Right(InstructionRequest(optInstruction.get, optDomain.get, optHierarchy.get, elements, optInterface,
          mySecuredRequest))
      } else {
        Left(NotFound)
      }
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