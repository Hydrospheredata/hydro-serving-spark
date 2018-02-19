package io.hydrosphere.serving.grpc_spark.spark

import java.nio.file.{Files, Path}

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.spark_ml_serving.LocalPipelineModel
import org.apache.spark.ml.PipelineModel

case class SparkModel(pipelineModel: PipelineModel, modelContract: ModelContract)

object SparkModel {
  def load(modelPath: Path, modelDefPath: Path): SparkModel = {
    val pipeline = LocalPipelineModel.load(modelPath.toString)
    val contract = ModelContract.parseFrom(Files.newInputStream(modelDefPath))
    SparkModel(pipeline, contract)
  }
}
