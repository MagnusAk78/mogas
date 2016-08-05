import com.google.inject.AbstractModule

import models.services.OrganisationService
import models.services.OrganisationServiceImpl
import models.daos.FileDAO
import models.daos.FileDAOImpl
import models.services.FileServiceImpl
import models.services.FileService
import controllers.actions.GeneralActionsImpl
import controllers.actions.GeneralActions
import controllers.filters.LoggingFilterImpl
import controllers.filters.LoggingFilter
import models.daos.UserDAO
import models.daos.UserDAOImpl
import models.daos.OrganisationDAO
import models.daos.OrganisationDAOImpl
import models.daos.ElementDAO
import models.daos.FactoryDAOImpl
import models.daos.FactoryDAO
import models.daos.InterfaceDAOImpl
import models.daos.InterfaceDAO
import models.daos.HierarchyDAOImpl
import models.daos.ElementDAOImpl
import models.daos.HierarchyDAO
import models.services.FactoryService
import models.services.FactoryServiceImpl
import models.daos.InstructionDAO
import models.daos.InstructionDAOImpl
import models.daos.InstructionPartDAOImpl
import models.daos.InstructionPartDAO
import models.services.InstructionServiceImpl
import models.services.InstructionService
import models.services.AmlObjectServiceImpl
import models.services.AmlObjectService
import models.services.IssueService
import models.services.IssueServiceImpl
import models.daos.IssueUpdateDAOImpl
import models.daos.IssueDAOImpl
import models.daos.IssueDAO
import models.daos.IssueUpdateDAO

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {

    //Filters
    bind(classOf[LoggingFilter]).to(classOf[LoggingFilterImpl]).asEagerSingleton()

    //Actions
    bind(classOf[GeneralActions]).to(classOf[GeneralActionsImpl]).asEagerSingleton()

    //DAOs
    bind(classOf[UserDAO]).to(classOf[UserDAOImpl]).asEagerSingleton()
    bind(classOf[OrganisationDAO]).to(classOf[OrganisationDAOImpl]).asEagerSingleton()
    bind(classOf[FileDAO]).to(classOf[FileDAOImpl]).asEagerSingleton()
    bind(classOf[FactoryDAO]).to(classOf[FactoryDAOImpl]).asEagerSingleton()
    bind(classOf[HierarchyDAO]).to(classOf[HierarchyDAOImpl]).asEagerSingleton()
    bind(classOf[ElementDAO]).to(classOf[ElementDAOImpl]).asEagerSingleton()
    bind(classOf[InterfaceDAO]).to(classOf[InterfaceDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionDAO]).to(classOf[InstructionDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionPartDAO]).to(classOf[InstructionPartDAOImpl]).asEagerSingleton()
    bind(classOf[IssueDAO]).to(classOf[IssueDAOImpl]).asEagerSingleton()
    bind(classOf[IssueUpdateDAO]).to(classOf[IssueUpdateDAOImpl]).asEagerSingleton()

    //Services, User service is in Silhouette module
    bind(classOf[OrganisationService]).to(classOf[OrganisationServiceImpl])
    bind(classOf[FactoryService]).to(classOf[FactoryServiceImpl]).asEagerSingleton()
    bind(classOf[AmlObjectService]).to(classOf[AmlObjectServiceImpl]).asEagerSingleton()
    bind(classOf[InstructionService]).to(classOf[InstructionServiceImpl]).asEagerSingleton()
    bind(classOf[IssueService]).to(classOf[IssueServiceImpl]).asEagerSingleton()
    bind(classOf[FileService]).to(classOf[FileServiceImpl])
  }

}
