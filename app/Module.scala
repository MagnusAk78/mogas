import com.google.inject.AbstractModule
import controllers.actions.{GeneralActions, GeneralActionsImpl}
import controllers.filters.{LoggingFilter, LoggingFilterImpl}
import models.daos._
import models.services._

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

    //DAOs, User DAO is in Silhouette module
    bind(classOf[FileDAO]).to(classOf[FileDAOImpl]).asEagerSingleton()
    bind(classOf[DomainDAO]).to(classOf[DomainDAOImpl]).asEagerSingleton()
    bind(classOf[HierarchyDAO]).to(classOf[HierarchyDAOImpl]).asEagerSingleton()
    bind(classOf[ElementDAO]).to(classOf[ElementDAOImpl]).asEagerSingleton()
    bind(classOf[InterfaceDAO]).to(classOf[InterfaceDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionDAO]).to(classOf[InstructionDAOImpl]).asEagerSingleton()
    bind(classOf[InstructionPartDAO]).to(classOf[InstructionPartDAOImpl]).asEagerSingleton()
    bind(classOf[IssueDAO]).to(classOf[IssueDAOImpl]).asEagerSingleton()
    bind(classOf[IssueUpdateDAO]).to(classOf[IssueUpdateDAOImpl]).asEagerSingleton()

    //Services, User service is in Silhouette module
    bind(classOf[DomainService]).to(classOf[DomainServiceImpl]).asEagerSingleton()
    bind(classOf[AmlObjectService]).to(classOf[AmlObjectServiceImpl]).asEagerSingleton()
    bind(classOf[InstructionService]).to(classOf[InstructionServiceImpl]).asEagerSingleton()
    bind(classOf[IssueService]).to(classOf[IssueServiceImpl]).asEagerSingleton()
    bind(classOf[FileService]).to(classOf[FileServiceImpl])
  }

}
