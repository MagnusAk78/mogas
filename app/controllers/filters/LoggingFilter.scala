package controllers.filters

import javax.inject.Inject
import javax.inject.Singleton
import akka.stream.Materializer
import play.api.mvc.{Result, RequestHeader, Filter}
import play.api.Logger
import play.api.routing.Router.Tags
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait LoggingFilter extends Filter

@Singleton
class LoggingFilterImpl @Inject() (implicit val mat: Materializer) extends LoggingFilter {
  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis

    nextFilter(requestHeader).map { result =>

      val action = try {
        requestHeader.tags(Tags.RouteController) +
        "." + requestHeader.tags(Tags.RouteActionMethod)
      } catch {
        case _: Throwable => println("< No parsable route >")
      }
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      Logger.info(s"LoggingFilter: ${action} took ${requestTime}ms and returned ${result.header.status}")

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}