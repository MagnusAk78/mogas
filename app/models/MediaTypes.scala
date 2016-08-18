package models

import viewdata.NavTypes._
import utils._

object MediaTypes {
  sealed trait MediaType {
    val stringValue: String
  }

  final case object MediaImage extends MediaType {
    override val stringValue = "MediaImage"
  }
  
  final case object MediaVideo extends MediaType {
    override val stringValue = "MediaVideo"
  }
    
  final case object UnknownType extends MediaType {
    override val stringValue = "UnknownType"
  }

  def fromString(modelType: String): MediaType = modelType match {
      case MediaImage.stringValue => MediaImage
      case MediaVideo.stringValue => MediaVideo
      case _ => UnknownType
      }
}