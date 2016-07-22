package controllers

import scala.annotation.implicitNotFound

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.joda.time.DateTime

import com.mohiva.play.silhouette.api.Silhouette
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.writer

import akka.stream.Materializer
import forms.OrganisationForm
import forms.OrganisationForm.fromOrganisationToData
import javax.inject.Inject
import javax.inject.Singleton
import models.Organisation
import models.daos.FileDAO
import models.services.FileService
import models.services.OrganisationService
import models.services.UserService
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc.BodyParser
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData
import play.modules.reactivemongo.JSONFileToSave
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.ReactiveMongoComponents
import reactivemongo.play.json.JsFieldBSONElementProducer
import utils.PaginateData
import utils.auth.DefaultEnv
import play.api.libs.json.JsValue
import models.Images
import models.Types
import play.api.mvc.Result
import java.io.File
import play.api.mvc.ActionRefiner
import com.mohiva.play.silhouette.api.actions.SecuredAction
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import reactivemongo.api.Cursor
import play.api.mvc.WrappedRequest
import controllers.actions.GeneralActions
import models.AmlFiles

@Singleton
class FileController @Inject() (
  val messagesApi: MessagesApi,
  val generalActions: GeneralActions,
  val organisationService: OrganisationService,
  val userService: UserService,
  val fileService: FileService,
  val reactiveMongoApi: ReactiveMongoApi,
  implicit val webJarAssets: WebJarAssets)(implicit exec: ExecutionContext, materialize: Materializer)
    extends Controller with MongoController with ReactiveMongoComponents with I18nSupport {

  import models.daos.FileDAO.JSONReadFile

  import MongoController.readFileReads

  def getStandardImage(uuid: String, modelTypeString: String) = generalActions.MySecuredAction async {
    implicit request =>
      // find the matching attachment, if any, and streams it to the client
      val serveResult = fileService.findByQuery(Images.getQueryImage(uuid, Images.Standard)).flatMap(cursor =>
        fileService.withAsyncGfs[Result] { gfs => serve[JsValue, JSONReadFile](gfs)(cursor, CONTENT_DISPOSITION_INLINE) })

      val response = serveResult.flatMap {
        _ match {
          case NotFound => {
            val modelType = models.Types.fromString(modelTypeString)

            Future.successful(Ok(views.html.ImageUpload(uuid, modelType, Some(request.identity),
              request.activeOrganisation)))

            //No image found, we need to send default image depending type
            /*
            modelType match {
              case models.Types.OrganisationType => Future.successful(Redirect(routes.Assets.at("images/organisation-default.png")).as("image/png"))
              case models.Types.UserType => Future.successful(Redirect(routes.Assets.at("images/user-default.png")).as("image/png"))
              case models.Types.FactoryType => Future.successful(Redirect(routes.Assets.at("images/factory-default.png")).as("image/png"))
              case models.Types.HierarchyType => Future.successful(Redirect(routes.Assets.at("images/hierarchy-default.png")).as("image/png"))
              case models.Types.InternalElementType => Future.successful(Redirect(routes.Assets.at("images/element-default.png")).as("image/png"))
              case models.Types.ExternalInterfaceType => Future.successful(Redirect(routes.Assets.at("images/interface-default.png")).as("image/png"))
              case models.Types.UnknownType => Future.successful(NotFound)
            }
            * 
            */
          }
          case _ => serveResult
        }
      }

      response
  }

  def uploadImage(uuid: String, modelTypeString: String) = generalActions.MySecuredAction { implicit request =>
    val modelType = models.Types.fromString(modelTypeString)
    Ok(views.html.ImageUpload(uuid, modelType, Some(request.identity), request.activeOrganisation))
  }

  def getThumbnailImage(uuid: String, modelTypeString: String) = generalActions.MySecuredAction async { implicit request =>
    // find the matching attachment, if any, and streams it to the client
    val serveResult = fileService.findByQuery(Images.getQueryImage(uuid, Images.Thumbnail)).flatMap(cursor =>
      fileService.withAsyncGfs[Result] { gfs => serve[JsValue, JSONReadFile](gfs)(cursor, CONTENT_DISPOSITION_INLINE) })

    val response = serveResult.flatMap {
      _ match {
        case NotFound => {
          //No image found, we need to send default image depending type
          val modelType = models.Types.fromString(modelTypeString)
          modelType match {
            case models.Types.OrganisationType =>
              Logger.info("Trying to send organisation-default")
              Future.successful(Redirect(routes.Assets.at("images/organisation-default.png")).as("image/png"))
            //Future.successful(Ok.sendFile(new File("/public/images/organisation-default.png"), true).as("image/png"))

            //Future.successful(Redirect(routes.Assets.at("images/organisation-default.png")).as("image/png"))
            case models.Types.UserType => Future.successful(Redirect(routes.Assets.at("images/user-default.png")).as("image/png"))
            case models.Types.FactoryType => Future.successful(Redirect(routes.Assets.at("images/factory-default.png")).as("image/png"))
            case models.Types.HierarchyType => Future.successful(Redirect(routes.Assets.at("images/hierarchy-default.png")).as("image/png"))
            case models.Types.InternalElementType => Future.successful(Redirect(routes.Assets.at("images/element-default.png")).as("image/png"))
            case models.Types.ExternalInterfaceType => Future.successful(Redirect(routes.Assets.at("images/interface-default.png")).as("image/png"))
            case models.Types.UnknownType => Future.successful(NotFound)
          }
        }
        case _ => serveResult
      }
    }

    response
  }

  def submitImage(uuid: String, modelType: String) = {

    //Create the parser using the GridFS collection from FileService
    val gdfsParser = fileService.withSyncGfs[BodyParser[MultipartFormData[Future[FileDAO.JSONReadFile]]]] { gfs => gridFSBodyParser(gfs) }

    generalActions.MySecuredAction.async(gdfsParser) { implicit request =>

      Logger.info("SignUpController: submitImage")

      //A list of the old images that now has to be removed
      val futureOldImageFileList = fileService.findByQuery(Images.getQueryAllImages(uuid)).flatMap(cursor => cursor.collect[List](0, true))

      val futureOptFileRef = request.body.file(forms.ImageFileKeyString) match {
        case Some(file) => file.ref.map(Some(_))
        case None => Future.successful(None)
      }
      //If the file is not an image or the correct size, just remove it
      val futureOptFile = futureOptFileRef.flatMap(optFileRef => optFileRef match {
        case Some(file) => {
          if (file.contentType.isEmpty ||
            file.contentType.get.startsWith("image") == false ||
            file.length < utils.DefaultValues.MinimumImageSize) {

            //File is not ok, remove it
            val result = fileService.remove(file.id.as[String])

            //And return nothing
            Future.successful(None)
          } else {

            Logger.info("File uploaded correctly")

            val iteratorUploaded1 = fileService.withAsyncGfs[Array[Byte]] { gfs => gfs.enumerate(file).run(Iteratee.consume[Array[Byte]]()) }

            val iteratorUploaded2 = fileService.withAsyncGfs[Array[Byte]] { gfs => gfs.enumerate(file).run(Iteratee.consume[Array[Byte]]()) }

            val futureSaveStandardResult = iteratorUploaded1.flatMap {
              bytes =>
                {
                  // Create standard image
                  val enumeratorStandard: Enumerator[Array[Byte]] = Enumerator.fromStream(Image(bytes).bound(Images.Standard.pixels, Images.Standard.pixels).stream)

                  val dataImage = JSONFileToSave(
                    filename = file.filename,
                    contentType = file.contentType,
                    uploadDate = Some(DateTime.now().getMillis),
                    metadata = Images.getImageMetadata(uuid, Images.Standard))

                  Logger.info("Saving the new large file")
                  fileService.save(enumeratorStandard, dataImage)
                }
            }

            val futureSaveThumbnailResult = iteratorUploaded2.flatMap {
              bytes =>
                {
                  val enumeratorThumbnail: Enumerator[Array[Byte]] = Enumerator.fromStream(Image(bytes).bound(Images.Thumbnail.pixels, Images.Thumbnail.pixels).stream)

                  val dataThumbnail = JSONFileToSave(
                    filename = file.filename,
                    contentType = file.contentType,
                    uploadDate = Some(DateTime.now().getMillis),
                    metadata = Images.getImageMetadata(uuid, Images.Thumbnail))

                  Logger.info("Saving the new thumbnail file")
                  fileService.save(enumeratorThumbnail, dataThumbnail)
                }
            }

            val saveResponses = for {
              saveStandardResult <- futureSaveStandardResult
              saveThumbnailResult <- futureSaveThumbnailResult
              removeResult <- {
                fileService.remove(file.id.as[String])
              }
            } yield removeResult

            saveResponses recover {
              case e => {
                Logger.error("recover e.getStackTrace: " + e.getStackTrace)
                InternalServerError(e.getMessage())
              }
            }
          }
        }
        case None => {
          Logger.info("optFileRef == None")
          Future.successful(None)
        }
      })

      val futureActiveOrg = organisationService.findOneByUuid(request.identity.activeOrganisation)

      val responses = for {
        optFile <- futureOptFile
        activeOrg <- futureActiveOrg
        oldImageFileList <- futureOldImageFileList
        removeOldImagesRes <- Future.sequence(oldImageFileList.map { file =>

          Logger.info("Removing old image, oldImageUuid: " + file.id.as[String])

          fileService.remove(file.id.as[String])
        })
      } yield Types.fromString(modelType) match {
        case Types.OrganisationType => Redirect(routes.OrganisationController.edit(uuid)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.UserType => Redirect(routes.SignUpController.edit(uuid)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.FactoryType => Redirect(routes.FactoryController.edit(uuid)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.HierarchyType => Redirect(routes.FactoryController.hierarchy(uuid, 1)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.InternalElementType => Redirect(routes.FactoryController.element(uuid, 1, 1)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.ExternalInterfaceType => Redirect(routes.FactoryController.interface(uuid)).flashing("success" -> Messages("db.success.imageUpload"))
        case Types.UnknownType => Redirect(routes.OrganisationController.list(1)).flashing("error" -> Messages("error.unknownType"))
      }

      responses recover {
        case e => {
          Logger.error("recover e.getStackTrace: " + e.getStackTrace)
          InternalServerError(e.getMessage())
        }
      }
    }
  }

  def submitAmlFile(factoryUuid: String) = {

    //Create the parser using the GridFS collection from FileService
    val gdfsParser = fileService.withSyncGfs[BodyParser[MultipartFormData[Future[FileDAO.JSONReadFile]]]] { gfs => gridFSBodyParser(gfs) }

    (generalActions.MySecuredAction andThen generalActions.RequireActiveOrganisation andThen
      generalActions.FactoryAction(factoryUuid)).async(gdfsParser) { implicit factoryRequest =>

        //A list of the existing aml files
        val futureExistingAmlFileList = fileService.findByQuery(AmlFiles.getQueryAllAmlFiles(factoryUuid)).flatMap(cursor => cursor.collect[List](0, true))

        val futureOptFileRef = factoryRequest.body.file(forms.AmlFileKeyString) match {
          case Some(file) => file.ref.map(Some(_))
          case None => Future.successful(None)
        }

        val responses = for {
          optFileRef <- futureOptFileRef
        } yield optFileRef match {
          case Some(file) => {
            if (file.contentType.isEmpty ||
              file.contentType.get != AmlFiles.OctetStreamContentType ||
              file.filename.isEmpty || !file.filename.get.endsWith("aml") ||
              file.length < utils.DefaultValues.MinimumImageSize) {

              //File is not ok, remove it
              val result = fileService.remove(file.id.as[String])

              //And return with error
              Future.successful(Redirect(routes.FactoryController.edit(factoryUuid)).flashing("error" -> Messages("amlFile.not.ok")))
            } else {
              Logger.info("Aml file uploaded correctly")

              fileService.updateMetadata(file.id.as[String], AmlFiles.getAmlFileMetadata(factoryUuid)).map { success =>
                Redirect(routes.FactoryController.edit(factoryUuid)).flashing("success" -> ("success: " + success))
              }
            }
          }
          case None => {
            Future.successful(Redirect(routes.FactoryController.edit(factoryUuid)).flashing("error" -> Messages("amlFile.upload.failed")))
          }
        }

        responses.flatMap { r =>

          r recover {
            case e => {
              Logger.error("recover e.getStackTrace: " + e.getStackTrace)
              InternalServerError(e.getMessage())
            }
          }
        }
      }
  }
}
