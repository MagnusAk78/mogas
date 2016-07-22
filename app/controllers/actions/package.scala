package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.auth.DefaultEnv
import models.Organisation
import play.api.mvc.WrappedRequest
import models.User
import play.api.mvc.Request
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.Authenticator
import models.Factory
import models.Hierarchy
import models.InternalElement
import models.ExternalInterface

package object actions {

  //This was the only way I could wrap the SecuredRequest as a Request since it takes 2 parameters with 'DefaultEnv'
  type SilhouetteSecuredRequest[A] = SecuredRequest[DefaultEnv, A]

  type SilhouetteUserAwareRequest[A] = UserAwareRequest[DefaultEnv, A]

  case class MySecuredRequest[A](
      activeOrganisation: Option[Organisation],
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
    lazy val activeOrganisation = request.activeOrganisation
  }

  case class OrganisationRequest[A](organisation: Organisation, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeOrganisation = request.activeOrganisation
  }

  case class FactoryRequest[A](factory: Factory, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeOrganisation = request.activeOrganisation
  }

  case class HierarchyRequest[A](factory: Factory, hierarchy: Hierarchy, request: MySecuredRequest[A])
      extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeOrganisation = request.activeOrganisation
  }

  case class ElementRequest[A](factory: Factory, hierarchy: Hierarchy, elementChain: List[InternalElement],
                               request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeOrganisation = request.activeOrganisation
  }

  case class InterfaceRequest[A](factory: Factory, hierarchy: Hierarchy, elementChain: List[InternalElement],
                                 interface: ExternalInterface, request: MySecuredRequest[A]) extends WrappedRequest[A](request) {
    lazy val identity: User = request.identity
    lazy val activeOrganisation = request.activeOrganisation
  }
}