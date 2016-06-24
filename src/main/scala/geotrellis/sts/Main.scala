package geotrellis.sts

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import geotrellis.spark.util._

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("geotrellis-simple-tile-server")

    // TODO: Configuration
    val host = "0.0.0.0"
    val port = 8085

    val service = system.actorOf(Props[TileServerActor], "geotrellis-simple-tile-service-actor")

    IO(Http) ! Http.Bind(service, host, port)
  }

  def s3ValueReader(bucket: String, key: String): ValueReader[LayerId] =
    S3ValueReader(bucket, key)

  def hadoopValueReader(path: String): ValueReader[LayerId] = {
    val sc = SparkUtils.createLocalSparkContext("local[1]", "tmp")
    val attributeStore = HadoopAttributeStore(path, sc.hadoopConfiguration)
    try {
      HadoopValueReader(attributeStore)
    } finally {
      sc.stop()
    }
  }
}
