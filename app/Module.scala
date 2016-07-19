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

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

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
    
    //Services, User service is in Silhouette module
    bind(classOf[OrganisationService]).to(classOf[OrganisationServiceImpl])
    bind(classOf[FileService]).to(classOf[FileServiceImpl])
    
  }

}
