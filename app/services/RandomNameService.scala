package services

import java.util.concurrent.atomic.AtomicInteger
import javax.inject._
import scala.util.Random

/**
 * This trait demonstrates how to create a component that is injected
 * into a controller. The trait represents a counter that returns a
 * incremented number each time it is called.
 */
trait RandomNameService {
  def firstName(): String
  def lastName(): String
}

/**
 * This class is a concrete implementation of the [[Counter]] trait.
 * It is configured for Guice dependency injection in the [[Module]]
 * class.
 *
 * This class has a `Singleton` annotation because we need to make
 * sure we only use one counter per application. Without this
 * annotation we would get a new instance every time a [[Counter]] is
 * injected.
 */
@Singleton
class RandomNameServiceImpl extends RandomNameService {  
  
  val rand = new Random(System.currentTimeMillis())
  
  private val firstNames = List("Magnus", "John", "Abraham", "Isaac", "Michael")
  
  private val lastNames = List("Akerman", "Anderson", "Smith", "Newton", "Schumacher", "Bolt")
  
  override def firstName(): String = firstNames(rand.nextInt(firstNames.length))
  
  override def lastName(): String = lastNames(rand.nextInt(lastNames.length))
}
