package controllers

import javax.inject._


import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._

import play.api.Configuration
import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.libs.functional.syntax._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

case class Summary(version: String, resourceTitle: String, resourceId: String, eadLocation: String, scope: String, biog: String)
case class Detail(cuid: String, title: String, url: String, resourceIdentifier: String, resourceTitle: String, summaryUrl: String)
case class Archiveit(title: String, extent: String, display_url: String)



@Singleton
class ComposersController @Inject()(config: Configuration)(cc: ControllerComponents)(ws: WSClient)(implicit ec:ExecutionContext) extends AbstractController(cc) {
  
  implicit val archiveitWrites: Writes[Archiveit] = (
  (JsPath \ "title").write[String] and
  (JsPath \ "extent").write[String] and
  (JsPath \ "display_url").write[String])(unlift(Archiveit.unapply))

  val aspaceUrl = config.getString("aspaceUrl").get
  val rootUrl = config.getString("rootUrl").get

  def summary(identifier: String) = Action.async { implicit request: Request[AnyContent] =>

	ws.url(aspaceUrl + "summary?resource_id=" + identifier).get().map { response =>
		val json = Json.parse(response.body)
		val version = json("version").as[String]
		val resourceTitle = json("resource_title").as[String]
		val resourceId = json("resource_identifier").as[String]
		val eadLocation = json("ead_location").as[String]
		val scope = json("scopecontent").as[String]
		val biog = json("bioghist").as[String]
		val summary = new Summary(version, resourceTitle,resourceId, eadLocation, scope, biog)
		val dos = json("digital_objects").as[Vector[JsObject]]
		Ok(views.html.summary(summary, dos, rootUrl))
	}
  }

  def detail(cuid: String) = Action.async { implicit request: Request[AnyContent] =>

  	ws.url(aspaceUrl + "detailed?component_id=" + cuid).get().map { response =>
  		val json = Json.parse(response.body)
  		val cuid = json("component_id").as[String]
  		val title = json("title").as[String]
  		val url = json("file_uris").as[Vector[String]]
  		val resourceIdentifier = json("resource_identifier").as[String]
  		val resourceTitle = json("resource_title").as[String]
  		val summary_url = (rootUrl + "summary/" + resourceIdentifier)
  		val dao = new Detail(cuid, title, url(0), resourceIdentifier, resourceTitle, summary_url)
  		Ok(views.html.detail(dao))
  	}
  }

  def archiveIt(identifier: String) = Action.async {
  	ws.url(aspaceUrl + "archiveit?resource_id=" + identifier).get().map { response => 
  		val json = Json.parse(response.body)
  		val archiveIt = new Archiveit(json("title").as[String], json("extent").as[String], json("display_url").as[String])
  		Ok(Json.toJson(archiveIt))
  	}
  }
}
