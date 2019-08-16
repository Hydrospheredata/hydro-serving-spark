package io.hydrosphere.serving.grpc_spark.spark

import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.spark_ml_serving.LocalPipelineModel


case class SparkModel(pipelineModel: LocalPipelineModel, predictSignature: ModelSignature)