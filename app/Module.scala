import com.google.inject.AbstractModule

import models.daos.OrganisationDAO
import models.daos.OrganisationDAOImpl
import models.daos.UserDAO
import models.daos.UserDAOImpl
import models.services.OrganisationService
import models.services.OrganisationServiceImpl

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
    
    //DAOs
    bind(classOf[UserDAO]).to(classOf[UserDAOImpl]).asEagerSingleton()
    bind(classOf[OrganisationDAO]).to(classOf[OrganisationDAOImpl]).asEagerSingleton()
    
    //Services, User service is in Silhouette module
    bind(classOf[OrganisationService]).to(classOf[OrganisationServiceImpl])
  }

}
