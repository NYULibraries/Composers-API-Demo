package controllers

import javax.inject._


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

case class Summary(version: String, resourceTitle: String, resourceId: String, eadLocation: String, scope: String, biog: String)
case class DetailParent(title: String, biogHist: Vector[String])
case class Detail(cuid: String, title: String, url: String, resourceIdentifier: String, resourceTitle: String, summaryUrl: String, parent: DetailParent)
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


  def testws() = Action.async { implicit request: Request[AnyContent] =>
    

      val r = ws.url(aspaceUrl + "summary?resource_id=mss.460")
      println(r.headers)
      r.get().map { response => 
      println(response.headers) 
      Ok("ok")
    }
  }

  def summary(identifier: String) = Action.async { implicit request: Request[AnyContent] =>

  	val request = ws.url(aspaceUrl + "summary?resource_id=" + identifier)
    request.withHttpHeaders("charset" -> "utf-8")

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

      for(doo <- dos) {
        doss = doss + ((doo \ "component_id").as[String] -> doo)
      }

  		Ok(views.html.summary(summary, ListMap(doss.toSeq.sortWith(_._1 < _._1):_*), rootUrl))
  	}



  }

  def detail(cuid: String) = Action.async { implicit request: Request[AnyContent] =>

  	ws.url(aspaceUrl + "detailed?component_id=" + cuid).get().map { response =>
  		val json = Json.parse(response.body)
      val record = json("ao")
      val parent = json("parent")
      val cuid = record("component_id").as[String]
  		val title = record("title").as[String]
  		val url = record("file_uris").as[Vector[String]]
  		val resourceIdentifier = record("resource_identifier").as[String]
  		val resourceTitle = record("resource_title").as[String]
  		val summary_url = (rootUrl + "summary/" + resourceIdentifier)
      val biogHist = parent("bioghist").as[Vector[String]]
      val pTitle = parent("title").as[String]
  		val dao = new Detail(cuid, title, url(0), resourceIdentifier, resourceTitle, summary_url, new DetailParent(pTitle, biogHist))
  		Ok(views.html.detail(dao))
  	}
  }

  def index() = Action {
    Ok(views.html.index())
  }
}
