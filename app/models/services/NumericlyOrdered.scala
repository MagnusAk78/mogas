package models.services

/**
 * Created by Magnus on 2015-11-14.
 */
trait NumericlyOrdered extends Ordered[NumericlyOrdered] {
  val orderNumber: Int

  def compare(that: NumericlyOrdered) =  this.orderNumber - that.orderNumber
}

object NumericlyOrdered {
  val orderNumberKey = "orderNumber"
}
