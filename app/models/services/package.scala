package models

import utils.PaginateData

package object services {
  
  trait ModelListData[M <: BaseModel] {
    val list: List[M]
    val paginateData: PaginateData
  }
}