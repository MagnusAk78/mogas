import com.google.inject.AbstractModule
import java.time.Clock

import services.{ApplicationTimer, AtomicCounter, Counter, MessageCounter, AtomicMessageCounter}
import services.RandomNameServiceImpl
import services.RandomNameService
import models.daos._
import models.services.ArticleService
import models.services.ArticleServiceImpl

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
    
    /**
     * Only for testing purposes
     */
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])
    // Set AtomicMessageCounter as the implementation for MessageCounter.
    bind(classOf[MessageCounter]).to(classOf[AtomicMessageCounter])
    
    /**
     * Reactive Mongo Stuff
     */
    bind(classOf[RandomNameService]).to(classOf[RandomNameServiceImpl])
    
    //DAOs
    bind(classOf[UserDAO]).to(classOf[UserDAOImpl]).asEagerSingleton()
    bind(classOf[ArticleDAO]).to(classOf[ArticleDAOImpl]).asEagerSingleton()
    
    //Services, User service is in Silhuette module
    bind(classOf[ArticleService]).to(classOf[ArticleServiceImpl])
  }

}
