package utils

case class PaginateData( 
  val page: Int,
  val count: Int,
  val pageLength: Int = utils.DefaultValues.DefaultPageLength
  ){}

