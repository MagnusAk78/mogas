package controllers

import java.io.{BufferedInputStream, ByteArrayInputStream, ByteArrayOutputStream}

import akka.stream.Materializer
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import com.sksamuel.scrimage.nio._
import controllers.actions.GeneralActions
import javax.inject.{Inject, Singleton}
import models._
import models.daos.FileDAO
import models.services.{DomainService, FileService, UserService}
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.libs.json.JsValue
import play.api.mvc._
import play.modules.reactivemongo.{JSONFileToSave, MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.Cursor
import viewdata._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileController @Inject()(
                                generalActions: GeneralActions,
                                userService: UserService,
                                fileService: FileService,
                                domainService: DomainService,
                                val reactiveMongoApi: ReactiveMongoApi,
                                components: ControllerComponents)
                              (implicit exec: ExecutionContext, materialize: Materializer)
  extends AbstractController(components) with MongoController with ReactiveMongoComponents with I18nSupport {

  implicit val lang: Lang = components.langs.availables.head

  implicit def color2awt(color: com.sksamuel.scrimage.color.Color): java.awt.Color = color.awt()

  val fileControllerLogger: Logger = Logger("FileController")

  import MongoController.readFileReads
  import models.daos.FileDAO.JSONReadFile

  def getStandardImage(uuid: String, modelTypeString: String) = generalActions.MySecuredAction async {
    implicit request =>
      // find the matching attachment, if any, and streams it to the client
      val serveResult = fileService.findByQuery(Images.getQueryImage(uuid, Images.Standard)).flatMap(cursor =>
        fileService.withAsyncGfs[Result]
          { gfs => serve[JsValue, JSONReadFile](gfs)(cursor, CONTENT_DISPOSITION_INLINE) })

      val response = serveResult.flatMap {
        _ match {
          case NotFound => {
            val modelType = models.Types.fromString(modelTypeString)

            //No image found, we need to send default image depending type
            modelType match {
              case models.Types.DomainType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
              }
              case models.Types.UserType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/user-default.png")).as("image/png"))
              }
              case models.Types.HierarchyType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/hierarchy-default.png")).as("image/png"))
              }
              case models.Types.ElementType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/element-default.png")).as("image/png"))
              }
              case models.Types.InterfaceType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/interface-default.png")).as("image/png"))
              }
              case models.Types.InstructionType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
              }
              case models.Types.InstructionPartType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
              }
              case models.Types.IssueType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
              }
              case models.Types.IssueUpdateType => {
                Future.successful(
                  Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
              }
              case models.Types.UnknownType => Future.successful(NotFound)
            }
          }
          case _ => serveResult
        }
      }

      response
  }

  def uploadImage(modelUuid: String, modelTypeString: String) = generalActions.MySecuredAction { implicit request =>
    Ok(views.html.imageUpload(
      GenericModelData(new DbModel with HasModelType {
        override val uuid: String = modelUuid
        override val modelType: String = Types.fromString(modelTypeString).stringValue
      }),
      UserStatus(Some(request.identity), request.activeDomain)))
  }

  def getThumbnailImage(uuid: String,
                        modelTypeString: String) = generalActions.MySecuredAction async { implicit request =>
    // find the matching attachment, if any, and streams it to the client
    val serveResult = fileService.findByQuery(Images.getQueryImage(uuid, Images.Thumbnail)).flatMap(cursor =>
      fileService.withAsyncGfs[Result] { gfs => serve[JsValue, JSONReadFile](gfs)(cursor, CONTENT_DISPOSITION_INLINE) })

    val response = serveResult.flatMap {
      _ match {
        case NotFound => {
          //No image found, we need to send default image depending type
          val modelType = models.Types.fromString(modelTypeString)

          fileControllerLogger.info("modelType: " + modelType)


          modelType match {
            case Types.DomainType => {
              Future.successful(
                Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
            }
            case Types.UserType => {
              Future.successful(
                Redirect(routes.Assets.at("images/user-default.png")).as("image/png"))
            }
            case Types.HierarchyType => {
              Future.successful(
                Redirect(routes.Assets.at("images/hierarchy-default.png")).as("image/png"))
            }
            case Types.ElementType => {
              Future.successful(
                Redirect(routes.Assets.at("images/element-default.png")).as("image/png"))
            }
            case Types.InterfaceType => {
              Future.successful(
                Redirect(routes.Assets.at("images/interface-default.png")).as("image/png"))
            }
            case Types.InstructionType => {
              Future.successful(
                Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
            }
            case Types.InstructionPartType => {
              Future.successful(
                Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
            }
            case models.Types.IssueType => {
              Future.successful(
                Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
            }
            case models.Types.IssueUpdateType => {
              Future.successful(
                Redirect(routes.Assets.at("images/domain-default.png")).as("image/png"))
            }
            case Types.UnknownType => Future.successful(NotFound)
          }
        }
        case _ => serveResult
      }
    }

    response
  }

  def uploadVideo(modelUuid: String, modelTypeString: String) = generalActions.MySecuredAction { implicit request =>
    Ok(views.html.videoUpload(GenericModelData(new DbModel with HasModelType {
      override val uuid: String = modelUuid
      override val modelType: String = Types.fromString(modelTypeString).stringValue
    }), UserStatus(Some(request.identity), request.activeDomain)))
  }

  def getVideo(uuid: String, modelTypeString: String) = generalActions.MySecuredAction async { implicit request =>
    fileService.findByQuery(Videos.getQueryAllVideos(uuid)).flatMap(cursor =>
      fileService.withAsyncGfs[Result] { gfs => serve[JsValue, JSONReadFile](gfs)(cursor, CONTENT_DISPOSITION_INLINE) })
  }

  def submitImage(uuid: String, modelType: String) = {

    //Create the parser using the GridFS collection from FileService
    val gdfsParser = fileService.withSyncGfs[BodyParser[MultipartFormData[FileDAO.JSONReadFile]]] {
      gfs => gridFSBodyParser(Future(gfs))
    }

    generalActions.MySecuredAction.async(gdfsParser) { implicit request =>

      fileControllerLogger.info("SignUpController: submitImage")

      //A list of the old images that now has to be removed
      val futureOldImageFileList = fileService.findByQuery(Images.getQueryAllImages(uuid)).
        flatMap(cursor => cursor.collect[List](-1, Cursor.FailOnError[List[JSONReadFile]]()))

      val optFile: Option[JSONReadFile] = request.body.file(models.formdata.ImageFileKeyString).map(_.ref)

      //If the file is not an image or the correct size, just remove it
      optFile match {
        case Some(file) => {
          if (file.contentType.isEmpty ||
            file.contentType.get.startsWith("image") == false ||
            file.length < utils.DefaultValues.MinimumFileSize) {

            //File is not ok, remove it
            val result = fileService.remove(file.id.as[String])

            //And return nothing
            Future.successful(None)
          } else {

            fileControllerLogger.info("File uploaded correctly")

            val futureBytesForStandard: Future[Array[Byte]] = fileService.withAsyncGfs[Array[Byte]] { gfs =>
              val stream = new ByteArrayOutputStream()
              val populateStream = gfs.readToOutputStream(file, stream)
              for {x <- populateStream} yield stream.toByteArray()
            }

            val futureBytesForThumbnail: Future[Array[Byte]] = futureBytesForStandard.map {
              bytes => bytes.clone()
            }

            val futureSaveStandardResult = futureBytesForStandard.flatMap {
              bytes => {
                // Create standard image
                val standardImageBytes: Array[Byte] = ImmutableImage.loader().fromBytes(bytes).
                  bound(Images.Standard.xPixels, Images.Standard.yPixels).bytes(new JpegWriter())

                val dataImage = JSONFileToSave(
                  filename = file.filename,
                  contentType = Some("image/jpeg"),
                  uploadDate = Some(DateTime.now().getMillis),
                  metadata = Images.getImageMetadata(uuid, Images.Standard))

                val inputStream = new BufferedInputStream(new ByteArrayInputStream(standardImageBytes))

                fileControllerLogger.info("Saving the new large file")
                fileService.save(inputStream, dataImage)
              }
            }

            val futureSaveThumbnailResult = futureBytesForThumbnail.flatMap {
              bytes => {
                val thumbnailImageBytes: Array[Byte] = ImmutableImage.loader().fromBytes(bytes).
                  fit(Images.Thumbnail.xPixels, Images.Thumbnail.yPixels, new RGBColor(0, 0, 0)).
                  bytes(new JpegWriter().withCompression(50))

                val dataThumbnail = JSONFileToSave(
                  filename = file.filename,
                  contentType = Some("image/jpeg"),
                  uploadDate = Some(DateTime.now().getMillis),
                  metadata = Images.getImageMetadata(uuid, Images.Thumbnail))

                val inputStream = new BufferedInputStream(new ByteArrayInputStream(thumbnailImageBytes))

                fileControllerLogger.info("Saving the new thumbnail file")
                fileService.save(inputStream, dataThumbnail)
              }
            }

            val saveResponses = for {
              saveStandardResult <- futureSaveStandardResult
              saveThumbnailResult <- futureSaveThumbnailResult
              removeResult <- {
                fileService.remove(file.id.as[String])
              }
            } yield {
              removeResult
            }
            saveResponses recover {
              case e => {
                fileControllerLogger.error("recover e.getStackTrace: " + e.getStackTrace)
                InternalServerError(e.getMessage())
              }
            }
          }
        }
        case None => {
          fileControllerLogger.info("optFileRef == None")
          Future.successful(None)
        }
      }

      val futureActiveOrg = domainService.findOneDomain(DbModel.queryByUuid(request.identity.activeDomain))

      val responses = for {
        //optFile <- futureOptFile
        activeDomain <- futureActiveOrg
        oldImageFileList <- futureOldImageFileList
        removeOldImagesRes <- Future.sequence(oldImageFileList.map { file =>

          fileControllerLogger.info("Removing old image, oldImageUuid: " + file.id.as[String])

          fileService.remove(file.id.as[String])
        })
      } yield {
        Types.fromString(modelType) match {
          case Types.DomainType => {
            Redirect(routes.DomainController.edit(uuid)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.UserType => {
            Redirect(routes.SignUpController.edit(uuid)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.HierarchyType => {
            Redirect(routes.DomainController.hierarchy(uuid, 1)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.ElementType => {
            Redirect(routes.DomainController.element(uuid, 1, 1)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.InterfaceType => {
            Redirect(routes.DomainController.interface(uuid)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.InstructionType => {
            Redirect(routes.InstructionController.instruction(uuid, 1)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.InstructionPartType => {
            Redirect(
              routes.InstructionController.showPart(uuid, 1, MediaTypes.MediaImage.stringValue)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.IssueType => {
            Redirect(routes.IssueController.issue(uuid, 1)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.IssueUpdateType => {
            Redirect(routes.IssueController.inspectIssueUpdate(uuid, 1)).flashing(
              "success" -> Messages("db.success.imageUpload"))
          }
          case Types.UnknownType => {
            Redirect(routes.DomainController.list(1)).flashing(
              "error" -> Messages("error.unknownType"))
          }
        }
      }
      responses recover {
        case e => {
          fileControllerLogger.error("recover e.getStackTrace: " + e.getStackTrace)
          InternalServerError(e.getMessage())
        }
      }
    }
  }

  def submitVideo(uuid: String, modelType: String) = {

    //Create the parser using the GridFS collection from FileService
    val gdfsParser = fileService.withSyncGfs[BodyParser[MultipartFormData[FileDAO.JSONReadFile]]] {
      gfs => gridFSBodyParser(Future(gfs))
    }

    generalActions.MySecuredAction.async(gdfsParser) { implicit request =>

      fileControllerLogger.info("SignUpController: submitVideo")

      //A list of the old videos that now has to be removed
      val futureOldVideoFileList = fileService.findByQuery(Videos.getQueryAllVideos(uuid)).
        flatMap(cursor => cursor.collect[List](-1, Cursor.FailOnError[List[JSONReadFile]]()))

      val optFile: Option[JSONReadFile] = request.body.file(models.formdata.VideoFileKeyString).map(_.ref)

      //If the file is not a video or the correct size, just remove it
      optFile match {
        case Some(file) => {
          if (file.contentType.isEmpty ||
            file.contentType.get.startsWith("video") == false ||
            file.length < utils.DefaultValues.MinimumFileSize) {

            //File is not ok, remove it
            val result = fileService.remove(file.id.as[String])

            //And return nothing
            Future.successful(None)
          } else {

            fileControllerLogger.info("File uploaded correctly")

            val futureBytes: Future[Array[Byte]] = fileService.withAsyncGfs[Array[Byte]] { gfs =>
              val stream = new ByteArrayOutputStream()
              val populateStream = gfs.readToOutputStream(file, stream)
              for {x <- populateStream} yield stream.toByteArray()
            }

            val futureSaveResult = futureBytes.flatMap {
              bytes => {
                val inputStream = new BufferedInputStream(new ByteArrayInputStream(bytes))

                val dataVideo = JSONFileToSave(
                  filename = file.filename,
                  contentType = file.contentType,
                  uploadDate = Some(DateTime.now().getMillis),
                  metadata = Videos.getVideoMetadata(uuid))

                fileControllerLogger.info("Saving the new large file")
                fileService.save(inputStream, dataVideo)
              }
            }


            val saveResponses = for {
              saveResult <- futureSaveResult
              removeResult <- fileService.remove(file.id.as[String])
            } yield {
              removeResult
            }
            saveResponses recover {
              case e => {
                fileControllerLogger.error("recover e.getStackTrace: " + e.getStackTrace)
                InternalServerError(e.getMessage())
              }
            }
          }
        }
        case None => {
          fileControllerLogger.info("optFileRef == None")
          Future.successful(None)
        }
      }

      val futureActiveOrg = domainService.findOneDomain(DbModel.queryByUuid(request.identity.activeDomain))

      val responses = for {
        //optFile <- futureOptFile
        activeDomain <- futureActiveOrg
        oldVideoFileList <- futureOldVideoFileList
        removeOldVideosRes <- Future.sequence(oldVideoFileList.map { file =>

          fileControllerLogger.info("Removing old video, id: " + file.id.as[String])

          fileService.remove(file.id.as[String])
        })
      } yield {
        Types.fromString(modelType) match {
          case Types.DomainType => {
            Redirect(routes.DomainController.edit(uuid)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.UserType => {
            Redirect(routes.SignUpController.edit(uuid)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.HierarchyType => {
            Redirect(routes.DomainController.hierarchy(uuid, 1)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.ElementType => {
            Redirect(routes.DomainController.element(uuid, 1, 1)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.InterfaceType => {
            Redirect(routes.DomainController.interface(uuid)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.InstructionType => {
            Redirect(routes.InstructionController.instruction(uuid, 1)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.InstructionPartType => {
            Redirect(
              routes.InstructionController.showPart(uuid, 1, MediaTypes.MediaVideo.stringValue)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.IssueType => {
            Redirect(routes.IssueController.issue(uuid, 1)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.IssueUpdateType => {
            Redirect(routes.IssueController.inspectIssueUpdate(uuid, 1)).flashing(
              "success" -> Messages("db.success.videoUpload"))
          }
          case Types.UnknownType => {
            Redirect(routes.DomainController.list(1)).flashing(
              "error" -> Messages("error.unknownType"))
          }
        }
      }
      responses recover {
        case e => {
          fileControllerLogger.error("recover e.getStackTrace: " + e.getStackTrace)
          InternalServerError(e.getMessage())
        }
      }
    }
  }

  def submitAmlFile(domainUuid: String) = {

    //Create the parser using the GridFS collection from FileService
    val gdfsParser = fileService.withSyncGfs[BodyParser[MultipartFormData[FileDAO.JSONReadFile]]] {
      gfs => gridFSBodyParser(Future(gfs))
    }

    (generalActions.MySecuredAction andThen generalActions.DomainAction(domainUuid)).async(gdfsParser) {
      implicit domainRequest =>

        //A list of the existing aml files
        val futureExistingAmlFileList = fileService.findByQuery(AmlFiles.queryAllAmlFiles(domainUuid)).
          flatMap(cursor => cursor.collect[List](-1, Cursor.FailOnError[List[JSONReadFile]]()))

        val optFile = domainRequest.body.file(models.formdata.AmlFileKeyString).map(_.ref)

        val responses = for {
          existingAmlFileList <- futureExistingAmlFileList
        } yield {
          optFile match {
            case Some(file) => {
              if (file.contentType.isEmpty ||
                file.contentType.get != AmlFiles.OctetStreamContentType ||
                file.filename.isEmpty || !file.filename.get.endsWith("aml") ||
                file.length < utils.DefaultValues.MinimumFileSize) {

                //File is not ok, remove it
                val result = fileService.remove(file.id.as[String])

                //And return with error
                Future.successful(Redirect(routes.DomainController.edit(domainUuid)).
                  flashing("error" -> Messages("amlFile.not.ok")))
              } else {
                fileControllerLogger.info("Aml file uploaded correctly")

                fileService.updateMetadata(file.id.as[String], AmlFiles.amlFileMetadata(domainUuid)).map { success =>
                  Redirect(routes.DomainController.edit(domainUuid)).flashing("success" -> ("success: " + success))
                }
              }
            }
            case None => {
              Future.successful(Redirect(routes.DomainController.edit(domainUuid)).
                flashing("error" -> Messages("amlFile.upload.failed")))
            }
          }
        }
        responses.flatMap { r =>

          r recover {
            case e => {
              fileControllerLogger.error("recover e.getStackTrace: " + e.getStackTrace)
              InternalServerError(e.getMessage())
            }
          }
        }
    }
  }

  def removeAmlFile(domainUuid: String, fileUuid: String) =
    (generalActions.MySecuredAction).async { implicit mySecuredRequest =>

      val result = for {
        removeResult <- fileService.remove(fileUuid)
      } yield {
        removeResult.success match {
          case true => Redirect(routes.DomainController.edit(domainUuid)).flashing("success" -> Messages("fileRemoved"))
          case false => {
            Redirect(routes.DomainController.edit(domainUuid)).flashing(
              "error" -> removeResult.reason.getOrElse(""))
          }
        }
      }
      result recover {
        case e => InternalServerError(e.getMessage())
      }

    }
}
