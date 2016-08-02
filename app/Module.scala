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
import models.daos.InternalElementDAO
import models.daos.FactoryDAOImpl
import models.daos.FactoryDAO
import models.daos.ExternalInterfaceDAOImpl
import models.daos.ExternalInterfaceDAO
import models.daos.HierarchyDAOImpl
import models.daos.InternalElementDAOImpl
import models.daos.HierarchyDAO
import models.services.FactoryService
import models.services.HierarchyServiceImpl
import models.services.HierarchyService
import models.services.FactoryServiceImpl
import models.services.InternalElementService
import models.services.ExternalInterfaceService
import models.services.ExternalInterfaceServiceImpl
import models.services.InternalElementServiceImpl
import models.daos.InstructionDAO
import models.daos.InstructionDAOImpl
import models.daos.InstructionPartDAOImpl
import models.daos.InstructionPartDAO
import models.services.InstructionServiceImpl
import models.services.InstructionService

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
    bind(classOf[InternalElementDAO]).to(classOf[InternalElementDAOImpl]).asEagerSingleton()
    bind(classOf[ExternalInterfaceDAO]).to(classOf[ExternalInterfaceDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionDAO]).to(classOf[InstructionDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionPartDAO]).to(classOf[InstructionPartDAOImpl]).asEagerSingleton()

    //Services, User service is in Silhouette module
    bind(classOf[OrganisationService]).to(classOf[OrganisationServiceImpl])
    bind(classOf[FileService]).to(classOf[FileServiceImpl])
    bind(classOf[FactoryService]).to(classOf[FactoryServiceImpl]).asEagerSingleton()
    bind(classOf[HierarchyService]).to(classOf[HierarchyServiceImpl]).asEagerSingleton()
    bind(classOf[InternalElementService]).to(classOf[InternalElementServiceImpl]).asEagerSingleton()
    bind(classOf[ExternalInterfaceService]).to(classOf[ExternalInterfaceServiceImpl]).asEagerSingleton()
    bind(classOf[InstructionService]).to(classOf[InstructionServiceImpl]).asEagerSingleton()
  }

}
