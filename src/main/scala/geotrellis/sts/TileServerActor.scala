package geotrellis.sts

import geotrellis.raster._
import geotrellis.raster.render.ColorMap
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.ValueReader

import geotrellis.raster.histogram.Histogram
import scala.concurrent.Future

import akka.actor._
import scala.util.Try
import spray.caching._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.http.{HttpMethods, HttpMethod, HttpResponse, AllOrigins}
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.routing._


//class TileServerActor(val valueReader: ValueReader[LayerId]) extends Actor with TileServerRoutes {
class TileServerActor() extends Actor with TileServerRoutes {
  override def actorRefFactory: ActorContext = context
  override def receive = runRoute(serviceRoute)

  val valueReader = Main.hadoopValueReader("/gttiles")
//  val valueReader = Main.s3ValueReader("azavea-datahub", "catalog")
}

trait TileServerRoutes extends HttpService {
  def valueReader: ValueReader[LayerId]

  implicit val executionContext = actorRefFactory.dispatcher

  val tileReaderStore: Cache[Reader[SpatialKey, Tile]] = LruCache(initialCapacity = 10)

  val colorMapStore: Cache[ColorMap] = LruCache(initialCapacity = 10)

  def getTile(id: LayerId, key: SpatialKey): Future[Tile] =
    tileReaderStore(s"${id.name}/${id.zoom}") {
      valueReader.reader[SpatialKey, Tile](id)
    }.map { reader =>
      reader.read(key)
    }

  def getColorMap(colorMapString: String): Future[ColorMap] =
    colorMapStore(colorMapString) {
      ColorMap.fromString(colorMapString).get
    }

  def serviceRoute =
    get {
      pathPrefix("tms")(tms)
    }

  val missingTileHandler = ExceptionHandler {
    case ex: TileNotFoundError => respondWithStatus(404) {
      complete(s"No tile: ${ex.getMessage}")
    }
  }

  /** Fetch information about a Tile, or the Tile itself as a PNG */
  def tms = handleExceptions(missingTileHandler) {
    pathPrefix(Segment / IntNumber / IntNumber / IntNumber) { (layer, zoom, x, y) =>
      val key = SpatialKey(x, y)
      val layerId = LayerId(layer, zoom)

      get {
        parameters(
          'reclass, 'colorMap
        ) { (reclassMapString, colorMapString) =>
          import geotrellis.raster._

          // Parse reclass map. "value;reclass,value;reclass,..."
          val reclassMap =
            reclassMapString
              .split(",")
              .map { str =>
                val arr = str.split(";").map(_.toInt).toArray
                (arr(0), arr(1))
              }
              .toMap

          respondWithMediaType(MediaTypes.`image/png`) {
            complete {
              getTile(layerId, key).flatMap { tile =>
                getColorMap(colorMapString).map { colorMap =>
                  val reclassed =
                    tile.map { z =>
                      if(isData(z)) {
                        reclassMap.get(z) match {
                          case Some(v) => v
                          case None => 1
                        }
                      } else { NODATA }
                    }

                  Some(reclassed.renderPng(colorMap).bytes)
                }
              }
            }
          }
        }
      }
    }
  }
}
