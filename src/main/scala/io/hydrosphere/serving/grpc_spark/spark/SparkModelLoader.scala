package io.hydrosphere.serving.grpc_spark.spark

import java.nio.file.{Files, Path}

import cats.effect.Sync
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.spark_ml_serving.LocalPipelineModel


trait SparkModelLoader[F[_]] {
  def load(modelPath: Path, contractPath: Path): F[SparkModel]
}

object SparkModelLoader {
  def mkDefault[F[_]]()(implicit F: Sync[F]): SparkModelLoader[F] = {
    new SparkModelLoader[F] {
      override def load(modelPath: Path, contractPath: Path): F[SparkModel] = {
        for {
          pipeline <- F.delay(LocalPipelineModel.load(modelPath.toString))
          localPipeline <- F.delay(LocalPipelineModel.toLocal(pipeline))
          contract <- F.delay(ModelContract.parseFrom(Files.newInputStream(contractPath)))
          signature <- F.fromOption(contract.predict, new IllegalArgumentException("Contract doesn't have prediction signature"))
        } yield SparkModel(localPipeline, signature)
      }
    }
  }

  def mkConventionWrapped[F[_]](sLoader: SparkModelLoader[F])(implicit F: Sync[F]) = { modelPath: Path =>
    for {
      filesPath <- F.delay(modelPath.resolve("files"))
      contractPath <- F.delay(modelPath.resolve("contract.protobin"))
      res <- sLoader.load(filesPath, contractPath)
    } yield res
  }
}