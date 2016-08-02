package models

import utils.PaginateData

package object services {

  trait ModelListData[M <: DbModel] {
    val list: List[M]
    val paginateData: PaginateData
  }
}