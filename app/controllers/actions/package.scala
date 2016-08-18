package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.auth.DefaultEnv
import play.api.mvc.WrappedRequest
import models.User
import play.api.mvc.Request
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.Authenticator
import models.Domain
import models.Hierarchy
import models.Element
import models.Interface
import models.Instruction
import models.InstructionPart
import models.IssueUpdate
import models.Issue

package object actions {

  //This was the only way I could wrap the SecuredRequest as a Request since it takes 2 parameters with 'DefaultEnv'
  type SilhouetteSecuredRequest[A] = SecuredRequest[DefaultEnv, A]

  type SilhouetteUserAwareRequest[A] = UserAwareRequest[DefaultEnv, A]

  case class MySecuredRequest[A](
      activeDomain: Option[Domain],
      request: SilhouetteSecuredRequest[A]) extends WrappedRequest[A](request) {
    def identity: User = request.identity
    def authenticator = request.authenticator
  }

  case class MyUserAwareRequest[A](
      request: SilhouetteUserAwareRequest[A]) extends WrappedRequest[A](request) {
    def identity: Option[User] = request.identity
  }

  case class UserRequest[A](user: User, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class DomainRequest[A](myDomain: Domain, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class HierarchyRequest[A](myDomain: Domain, hierarchy: Hierarchy, request: MySecuredRequest[A])
      extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class ElementRequest[A](myDomain: Domain, hierarchy: Hierarchy, elementChain: List[Element],
      request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class InterfaceRequest[A](myDomain: Domain, hierarchy: Hierarchy, elementChain: List[Element],
      interface: Interface, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class AmlObjectRequest[A](myDomain: Domain, hierarchy: Hierarchy, elementChain: List[Element],
      interface: Option[Interface], request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain

    lazy val elementOrInterfaceUuid = interface.map(i => i.uuid).getOrElse(elementChain.last.uuid)
  }

  case class InstructionRequest[A](instruction: Instruction, myDomain: Domain, hierarchy: Hierarchy,
    elementChain: List[Element], interface: Option[Interface], request: MySecuredRequest[A])
      extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class InstructionPartRequest[A](instructionPart: InstructionPart, instruction: Instruction,
      request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class IssueRequest[A](issue: Issue, myDomain: Domain, hierarchy: Hierarchy,
    elementChain: List[Element], interface: Option[Interface], request: MySecuredRequest[A]) 
      extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }

  case class IssueUpdateRequest[A](issueUpdate: IssueUpdate, issue: Issue,
      request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeDomain = request.activeDomain
  }
}