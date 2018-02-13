package io.hydrosphere.serving.grpc_spark

import io.grpc.netty.NettyServerBuilder
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.ExecutionContext

object Main extends App {
  val infImpl = new InferenceServiceImpl("/model")
  val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
  val port = sys.env.get("APP_PORT").map(_.toInt).getOrElse(9090)
  val server = NettyServerBuilder.forPort(port).addService(service).build()
  val x = server.start()
  println(s"Server started at $port port")
  x.awaitTermination()
  println(s"Terminated")
}
