package viewdata

import models.DbModel

trait ModelListData[+M <: DbModel] {
  val list: List[M]
  val imageList: List[Boolean]
  val videoList: List[Boolean]
  val paginateData: PaginateData
}