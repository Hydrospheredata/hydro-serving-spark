package io.hydrosphere.serving.grpc_spark

import hydrosphere.tensorflow.serving.prediction_service.PredictionServiceGrpc
import io.grpc.ServerBuilder

import scala.concurrent.ExecutionContext

object Main extends App {
  val infImpl = new InferenceServiceImpl(
    "/Users/bulat/Documents/Dev/Provectus/hydro-serving-runtime/models/word2vec",
    "word2vec.protobin")
  val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
  val server = ServerBuilder.forPort(9090).addService(service).build()
  server.start()
  println("Server started!")
  server.awaitTermination()
}
