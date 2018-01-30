package io.hydrosphere.serving.grpc_spark

import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.grpc.ServerBuilder

import scala.concurrent.ExecutionContext

object Main extends App {
  val infImpl = new InferenceServiceImpl("/model")
  val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
  val server = ServerBuilder.forPort(9090).addService(service).build()
  server.start()
  println("Server started!")
  server.awaitTermination()
}
