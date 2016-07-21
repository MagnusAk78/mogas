package models

import utils.PaginateData

package object services {
  
  trait ModelListData[M <: DbModel] {
    val list: List[M]
    val paginateData: PaginateData
  }
  
  val set1: Set[String] = Set("first") ++ Some("bla").map(str => Set(str)).getOrElse(Set.empty)
  
  val set2: Set[String] = Set("second") ++ Some("blovv").map(str => Set(str)).getOrElse(Set.empty)
  
  val zipped = (set1 zip set2).map{tuple: Tuple2[String, String] => tuple._2}
}