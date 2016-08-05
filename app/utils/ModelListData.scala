package utils

import models.DbModel

trait ModelListData[M <: DbModel] {
  val list: List[M]
  val paginateData: PaginateData
}