package io.hydrosphere.serving.grpc_spark

import com.google.protobuf.empty.Empty
import io.hydrosphere.serving.grpc_spark.spark.SparkModel
import io.hydrosphere.serving.grpc_spark.util.TensorUtils
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.{PredictionServiceGrpc, StatusResponse}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

object ModelInferenceService {
  def serve(request: PredictRequest, sparkModel: SparkModel): PredictResponse = {
    val inputDF = TensorUtils.requestToLocalData(request.inputs, sparkModel.predictSignature)
    val res = sparkModel.pipelineModel.transform(inputDF)
    TensorUtils.localDataToResult(sparkModel.predictSignature, res)
  }

  def mkForModel(sparkModel: SparkModel): PredictionServiceGrpc.PredictionService = {
    val logger: Logger = LoggerFactory.getLogger(ModelInferenceService.getClass)
    new PredictionServiceGrpc.PredictionService {
      override def predict(request: PredictRequest): Future[PredictResponse] = {
        logger.debug("Prediction Request: {}", request)
        val check = request.inputs.forall {
          case (name, tensor) =>
            sparkModel.predictSignature.inputs.exists { ct =>
              ct.typeOrSubfields.dtype.exists(tInfo => ct.name == name && tInfo == tensor.dtype)
            }
        }
        if (check) {
          val result = serve(request, sparkModel)
          logger.debug("Prediction ok")
          Future.successful(result)
        } else {
          logger.debug("Model contract violation")
          Future.failed(new IllegalArgumentException(s"Model contract violation"))
        }
      }

      override def status(request: Empty): Future[StatusResponse] = Future.successful(
        StatusResponse(
          status = StatusResponse.ServiceStatus.SERVING,
          message = "ok"
        )
      )
    }
  }
}

