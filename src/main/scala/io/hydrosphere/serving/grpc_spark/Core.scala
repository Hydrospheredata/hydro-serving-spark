package io.hydrosphere.serving.grpc_spark

import java.nio.file.Path

import cats.effect.Sync
import cats.implicits._
import io.grpc.netty.NettyServerBuilder
import io.hydrosphere.serving.grpc_spark.spark.SparkModelLoader
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.ExecutionContext

object Core {
  def loadService[F[_]](modelPath: Path)(implicit F: Sync[F]) = {
    val loader = SparkModelLoader.mkDefault()
    val wrapped = SparkModelLoader.mkConventionWrapped(loader)
    for {
      model <- wrapped(modelPath)
    } yield ModelInferenceService.mkForModel(model)
  }

  def app[F[_]](modelPath: Path, port: Int)(implicit F: Sync[F]) = {
    for {
      service <- loadService(modelPath)
      serviceDef <- F.delay(PredictionServiceGrpc.bindService(service, ExecutionContext.global))
      server <- F.delay(NettyServerBuilder.forPort(port).addService(serviceDef).build())
    } yield server
  }
}
