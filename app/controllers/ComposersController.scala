package controllers

import javax.inject._
import java.io.ByteArrayInputStream

import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import scala.collection.immutable.ListMap

import play.api.Configuration
import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.i18n.I18nSupport

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import org.apache.commons.io.IOUtils
import java.nio.charset.StandardCharsets

case class Summary(version: String, resourceTitle: String, resourceId: String, eadLocation: String, scope: String, biog: String)
case class DetailParent(title: String, biogHist: Vector[String])
case class Detail(cuid: String, title: String, extent: Option[String], url: String, resourceIdentifier: String, resourceTitle: String, summaryUrl: String, parent: Option[DetailParent], accessRestrictions: Option[Vector[String]])
case class Archiveit(title: String, extent: String, display_url: String)

@Singleton
class ComposersController @Inject()(config: Configuration)(cc: ControllerComponents)(ws: WSClient)(implicit ec:ExecutionContext) extends AbstractController(cc) {
  
  val aspaceUrl = config.get[String]("aspaceUrl")
  val rootUrl = config.get[String]("rootUrl")

  def archiveIt(identifier: String) = Action.async {
    
    implicit val archiveitWrites: Writes[Archiveit] = (
      (JsPath \ "title").write[String] and
      (JsPath \ "extent").write[String] and
      (JsPath \ "display_url").write[String])(unlift(Archiveit.unapply))

    ws.url(aspaceUrl + "archiveit?resource_id=" + identifier).get().map { response => 
      val json = Json.parse(response.body)
      val archiveIt = new Archiveit(json("title").as[String], 
        json("extent").as[String], 
        rootUrl + "summary/" + identifier) 
      Ok(Json.toJson(archiveIt))
    }

  }

  def summary(identifier: String) = Action.async { implicit request: Request[AnyContent] =>

  	val request = ws.url(aspaceUrl + "summary?resource_id=" + identifier).addHttpHeaders("charset" -> "utf-8")
    var doss = Map[String, JsObject]()

    request.get().map { response =>
  		val json = Json.parse(response.body)
  		val version = json("version").as[String]
  		val resourceTitle = json("resource_title").as[String]
  		val resourceId = json("resource_identifier").as[String]
  		val eadLocation = json("ead_location").as[String]
  		val scope = json("scopecontent").as[String]
  		val biog = json("bioghist").as[String]
  		val summary = new Summary(version, resourceTitle,resourceId, eadLocation, scope, biog)
  		val dos = json("digital_objects").as[Vector[JsObject]]

      for(digital_obj <- dos) {
        doss = doss + ((digital_obj \ "component_id").as[String] -> digital_obj)
      }

  		Ok(views.html.summary(summary, ListMap(doss.toSeq.sortWith(_._1 < _._1):_*), rootUrl))

  	}

  }

  def detail(cuid: String) = Action.async { implicit request: Request[AnyContent] =>

    ws.url(aspaceUrl + "detailed?component_id=" + cuid).get().map { response =>
      val json = Json.parse(response.body)
      val ao = json("archival_object")
      val cuid = ao("component_id").as[String]
      val title = ao("title").as[String]
      val extent = ao("extent").asOpt[String]
      val urls = ao("file_uris").as[Vector[String]]
      val resourceIdentifier = ao("resource_identifier").as[String]
      val resourceTitle = ao("resource_title").as[String]
      val summary_url = (rootUrl + "summary/" + resourceIdentifier)

      ao("restrictions_apply").as[Boolean] match {
        case true => {
          val accessRestrictions = ao("accessrestrict").as[Vector[String]]
          val dao = new Detail(cuid, title, extent, urls(0), resourceIdentifier, resourceTitle, summary_url, getParent(json("parent_object").as[JsValue]), Some(accessRestrictions))
          Ok(views.html.restricted(dao))
        }

        case false => {
          val dao = new Detail(cuid, title, extent, urls(0), resourceIdentifier, resourceTitle, summary_url, getParent(json("parent_object").as[JsValue]), None)
          Ok(views.html.detail(dao))
        }
      }
    }
  }

  def getParent(jsValue: JsValue): Option[DetailParent] = {
    (jsValue != JsNull) match {
      case true => Some(new DetailParent(jsValue("title").as[String], jsValue("bioghist").as[Vector[String]]))
      case false => None
    }
  }

  def index() = Action {
    Ok(views.html.index())
  }
}
