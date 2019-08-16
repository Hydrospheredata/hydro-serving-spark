package io.hydrosphere.serving.grpc_spark

import java.nio.file.Paths

import cats.effect.IO
import org.slf4j.LoggerFactory

object Main extends App {
  val logger = LoggerFactory.getLogger(Main.getClass)
  val port = sys.env.get("APP_PORT").map(_.toInt).getOrElse(9090)

  val flow = for {
    server <- Core.app[IO](Paths.get("/model"), port)
  } yield {
    val x = server.start()
    logger.info(s"Server started at $port port")
    x.awaitTermination()
    logger.info(s"Terminated")
  }
  flow.unsafeRunSync()
}
