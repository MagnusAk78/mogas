import com.google.inject.AbstractModule

import models.services.OrganisationService
import models.services.OrganisationServiceImpl
import models.daos.FileDAO
import models.daos.FileDAOImpl
import models.services.FileServiceImpl
import models.services.FileService
import models.daos.ModelDAO
import models.daos.ModelDAOImpl

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
    bind(classOf[ModelDAO]).to(classOf[ModelDAOImpl]).asEagerSingleton()
    bind(classOf[FileDAO]).to(classOf[FileDAOImpl]).asEagerSingleton()
    
    //Services, User service is in Silhouette module
    bind(classOf[OrganisationService]).to(classOf[OrganisationServiceImpl])
    bind(classOf[FileService]).to(classOf[FileServiceImpl])
    
  }

}
