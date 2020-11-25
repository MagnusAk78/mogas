package controllers.actions

import com.mohiva.play.silhouette.api.Silhouette
import controllers.AlwaysAuthorized
import javax.inject.{Inject, Singleton}
import models._
import models.services._
import play.api.mvc.Results.NotFound
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

trait GeneralActions {

  def MySecuredAction: ActionBuilder[MySecuredRequest, AnyContent]

  def RequireActiveDomain: ActionFilter[MySecuredRequest]

  def MyUserAwareAction: ActionBuilder[MyUserAwareRequest, AnyContent]

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
    val silhouette: Silhouette[DefaultEnv])(implicit val ec: ExecutionContext) extends GeneralActions {

  def ActiveDomainAction = new ActionTransformer[SilhouetteSecuredRequest, MySecuredRequest] {
    def executionContext = ec
    override def transform[A](request: SilhouetteSecuredRequest[A]) = {
      domainService.findOneDomain(DbModel.queryByUuid(request.identity.activeDomain)).map { optActiveOrg =>
        MySecuredRequest(optActiveOrg, request)
      }
    }
  }

  def MyUserAwareTransformer = new ActionTransformer[SilhouetteUserAwareRequest, MyUserAwareRequest] {
    def executionContext = ec
    override def transform[A](request: SilhouetteUserAwareRequest[A]) = Future.successful(MyUserAwareRequest(request))
  }

  override def MySecuredAction =
    silhouette.SecuredAction(AlwaysAuthorized()) andThen ActiveDomainAction

  override def MyUserAwareAction = silhouette.UserAwareAction andThen MyUserAwareTransformer

  override def RequireActiveDomain: ActionFilter[MySecuredRequest] = new ActionFilter[MySecuredRequest] {
    def executionContext = ec
    override def filter[A](mySecuredRequest: MySecuredRequest[A]): Future[Option[Result]] =
      Future.successful(mySecuredRequest.activeDomain.map(activeDomain => None).getOrElse(Some(NotFound)))
  }

  override def UserAction(uuid: String) = new ActionRefiner[MySecuredRequest, UserRequest] {
    def executionContext = ec
    override def refine[A](activeDomainRequest: MySecuredRequest[A]) = {
      userService.findOne(DbModel.queryByUuid(uuid)).map { optUser =>
        optUser.map(
          UserRequest(_, activeDomainRequest)).toRight(NotFound)
      }
    }
  }

  override def DomainAction(uuid: String) = new ActionRefiner[MySecuredRequest, DomainRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionDomain <- domainService.findOneDomain(DbModel.queryByUuid(uuid))
      } yield optionDomain.map(domain => DomainRequest(domain, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def HierarchyAction(uuid: String) = new ActionRefiner[MySecuredRequest, HierarchyRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(uuid))
        optionDomain <- domainService.findOneDomain(DbModel.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => HierarchyRequest(domain, optionHierarchy.get, mySecuredRequest))
        .toRight(NotFound)
    }
  }

  override def ElementAction(uuid: String) = new ActionRefiner[MySecuredRequest, ElementRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        elements <- amlObjectService.getElementChain(uuid)
        optionHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optionDomain <- domainService.findOneDomain(DbModel.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => ElementRequest(optionDomain.get, optionHierarchy.get, elements,
        mySecuredRequest)).toRight(NotFound)
    }
  }

  override def InterfaceAction(uuid: String) = new ActionRefiner[MySecuredRequest, InterfaceRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        interface <- amlObjectService.findOneInterface(DbModel.queryByUuid(uuid))
        elements <- amlObjectService.getElementChain(interface.get.parent)
        optionHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optionDomain <- domainService.findOneDomain(DbModel.queryByUuid(optionHierarchy.get.parent))
      } yield optionDomain.map(domain => InterfaceRequest(optionDomain.get, optionHierarchy.get, elements,
        interface.get, mySecuredRequest)).toRight(NotFound)
    }
  }

  override def AmlObjectAction(uuid: String) = new ActionRefiner[MySecuredRequest, AmlObjectRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optInterface <- amlObjectService.findOneInterface(DbModel.queryByUuid(uuid))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(DbModel.queryByUuid(uuid))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(DbModel.queryByUuid(optHierarchy.get.parent))
      } yield if (optHierarchy.isDefined && optDomain.isDefined) {
        Right(AmlObjectRequest(optDomain.get, optHierarchy.get, elements, optInterface, mySecuredRequest))
      } else {
        Left(NotFound)
      }
    }
  }

  override def InstructionAction(uuid: String) = new ActionRefiner[MySecuredRequest, InstructionRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optInstruction <- instructionService.findOneInstruction(DbModel.queryByUuid(uuid))
        optInterface <- amlObjectService.findOneInterface(DbModel.queryByUuid(optInstruction.get.parent))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(DbModel.queryByUuid(optInstruction.get.parent))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(DbModel.queryByUuid(optHierarchy.get.parent))
      } yield if (optInstruction.isDefined && optHierarchy.isDefined && optDomain.isDefined) {
        Right(InstructionRequest(optInstruction.get, optDomain.get, optHierarchy.get, elements, optInterface,
          mySecuredRequest))
      } else {
        Left(NotFound)
      }
    }
  }

  override def InstructionPartAction(uuid: String) = new ActionRefiner[MySecuredRequest, InstructionPartRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optInstructionPart <- instructionService.findOneInstructionPart(DbModel.queryByUuid(uuid))
        optInstruction <- instructionService.findOneInstruction(DbModel.queryByUuid(optInstructionPart.get.parent))
        optInterface <- amlObjectService.findOneInterface(DbModel.queryByUuid(optInstruction.get.parent))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(DbModel.queryByUuid(optInstruction.get.parent))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(DbModel.queryByUuid(optHierarchy.get.parent))
      } yield if (optInstructionPart.isDefined && optInstruction.isDefined && optHierarchy.isDefined && optDomain.isDefined) {
        Right(InstructionPartRequest(optInstructionPart.get, optInstruction.get, optDomain.get, optHierarchy.get, elements, 
            optInterface, mySecuredRequest))
      } else {
        Left(NotFound)
      }
    }
  }

  def IssueAction(uuid: String): ActionRefiner[MySecuredRequest, IssueRequest] = new ActionRefiner[MySecuredRequest, IssueRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionIssue <- issueService.findOneIssue(DbModel.queryByUuid(uuid))
        optInterface <- amlObjectService.findOneInterface(DbModel.queryByUuid(optionIssue.get.parent))
        optElement <- optInterface match {
          case Some(i) => Future.successful(None)
          case None => amlObjectService.findOneElement(DbModel.queryByUuid(optionIssue.get.parent))
        }
        elements <- optInterface match {
          case Some(interface) => amlObjectService.getElementChain(interface.parent)
          case None => amlObjectService.getElementChain(optElement.get.uuid)
        }
        optHierarchy <- domainService.findOneHierarchy(DbModel.queryByUuid(elements.head.parent))
        optDomain <- domainService.findOneDomain(DbModel.queryByUuid(optHierarchy.get.parent))
      } yield if (optionIssue.isDefined && optHierarchy.isDefined && optDomain.isDefined) {
        Right(IssueRequest(optionIssue.get, optDomain.get, optHierarchy.get, elements, optInterface,
          mySecuredRequest))
      } else {
        Left(NotFound)
      }
    }
  }

  def IssueUpdateAction(uuid: String): ActionRefiner[MySecuredRequest, IssueUpdateRequest] = new ActionRefiner[MySecuredRequest, IssueUpdateRequest] {
    def executionContext = ec
    override def refine[A](mySecuredRequest: MySecuredRequest[A]) = {
      for {
        optionIssueUpdate <- issueService.findOneIssueUpdate(DbModel.queryByUuid(uuid))
        optionIssue <- issueService.findOneIssue(DbModel.queryByUuid(optionIssueUpdate.get.parent))
      } yield (optionIssueUpdate, optionIssue) match {
        case (Some(issueUpdate), Some(issue)) => Right(IssueUpdateRequest(issueUpdate, issue, mySecuredRequest))
        case _ => Left(NotFound)
      }
    }
  }
}