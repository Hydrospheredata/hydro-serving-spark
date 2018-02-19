package io.hydrosphere.serving.grpc_spark

import java.nio.file.Paths

import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.grpc_spark.spark.SparkModel
import io.hydrosphere.spark_ml_serving.LocalPipelineModel

import scala.concurrent.Future

class InferenceServiceImpl(modelPath: String) extends PredictionServiceGrpc.PredictionService {


  val filesPath = s"$modelPath/files"
  val contractPath = s"$modelPath/contract.protobin"
  val sparkModel = SparkModel.load(Paths.get(filesPath), Paths.get(contractPath))
  val localModel: LocalPipelineModel = sparkModel.pipelineModel
  println(localModel)
  val sparkSignature = sparkModel.modelContract.signatures.head
  println(sparkSignature)

  override def predict(request: PredictRequest): Future[PredictResponse] = {
    println(request)
    val check = request.inputs.forall {
      case (name, tensor) =>
        sparkSignature.inputs.exists { ct =>
          val tInfo = ct.infoOrSubfields.info.getOrElse(throw new IllegalStateException("Runtime doesnt suport nested contracts"))
          ct.fieldName == name &&
            TensorUtils.areShapesCompatible(tInfo.tensorShape, tensor.tensorShape) &&
            tInfo.dtype == tensor.dtype
        }
    }

    if (check) {
      val result = serve(request)
      println(result)
      Future.successful(result)
    } else {
      println(s"Model contract violated with $request")
      Future.failed(new IllegalArgumentException(s"Model contract violated with $request"))
    }
  }

  def serve(request: PredictRequest): PredictResponse = {
    val inputDF = TensorUtils.requestToLocalData(request.inputs, sparkSignature)
    val res = localModel.transform(inputDF)
    TensorUtils.localDataToResult(sparkModel.modelContract, res)
  }
}