package viewdata

import models.DbModel
import models.Types
import models.HasModelType

case class GenericModelData(dbModel: DbModel with HasModelType) {
  lazy val navType = Types.fromString(dbModel.modelType).navType
}