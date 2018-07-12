package io.hydrosphere.serving.grpc_spark.util

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import io.hydrosphere.serving.tensorflow.api.predict.PredictResponse
import io.hydrosphere.serving.tensorflow.tensor.{TensorProto, TypedTensor, TypedTensorFactory}
import io.hydrosphere.spark_ml_serving.common.{LocalData, LocalDataColumn}
import org.slf4j.LoggerFactory

object TensorUtils {
  val logger = LoggerFactory.getLogger(TensorUtils.getClass)

  def verifyShape[T](tensor: TypedTensor[T]): TypedTensor[T] = {
    tensor.shape match {
      case AnyDims() => tensor
      case Dims(tensorDims, _) if tensorDims.isEmpty => tensor
      case Dims(tensorDims, _) =>
        if (tensorDims.isEmpty && tensor.data.length <= 1) {
          tensor
        } else {
          val reverseTensorDimIter = tensorDims.reverseIterator

          val actualDims = Array.fill(tensorDims.length)(0L)
          var actualDimId = actualDims.indices.last
          var dimLen = tensor.data.length

          var isShapeOk = true

          while (isShapeOk && reverseTensorDimIter.hasNext) {
            val currentDim = reverseTensorDimIter.next()
            val subCount = dimLen.toDouble / currentDim.toDouble
            if (subCount.isWhole()) { // ok
              dimLen = subCount.toInt
              if (subCount < 0) {
                actualDims(actualDimId) = dimLen.abs
              } else {
                actualDims(actualDimId) = currentDim
              }
              actualDimId -= 1
            } else { // not ok
              isShapeOk = false
            }
          }

          if (isShapeOk) {
            val rawTensor = tensor.toProto.copy(tensorShape = Dims(actualDims).toProto)
            val result = tensor.factory.fromProto(rawTensor)
            result
          } else {
            throw new IllegalArgumentException(s"Invalid shape $tensorDims for data ${tensor.data}")
          }
        }
    }
  }

  def verifyShape(tensor: TensorProto): TensorProto = {
    verifyShape(TypedTensorFactory.create(tensor)).toProto
  }

  def requestToLocalData(dataFrame: Map[String, TensorProto], modelSignature: ModelSignature): LocalData = {
    val cols = modelSignature.inputs.map { in =>
      val tensorType = in.typeOrSubfields.dtype.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      val sigData = dataFrame(in.name)
      if (tensorType == sigData.dtype) {
        tensorToLocalColumn(in.name, sigData)
      } else {
        throw new IllegalArgumentException(s"Expected $tensorType, got ${sigData.dtype}")
      }
    }
    LocalData(cols.toList)
  }

  def localDataToResult(modelContract: ModelContract, localData: LocalData): PredictResponse = {
    logger.debug("{}", localData)
    val sig = modelContract.signatures.head
    val localMap = localData.toMapList
    val rowTensors = sig.outputs.map { out =>
      val outType = out.typeOrSubfields.dtype.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      localData.column(out.name) match {
        case Some(column) =>
          val colShape = column.data.length
          val tensorFactory = TypedTensorFactory(outType)
          val colData = column.data.flatMap {
            case s: Seq[_] => s
            case x => Seq(x)
          }
          val maybeColTensor = tensorFactory.createFromAny(colData, TensorShape(out.shape))
          val colTensor = maybeColTensor.getOrElse(
            throw new IllegalArgumentException(s"Cant create tensor from $column")
          )
          out.name -> colTensor.toProto
        case None => throw new IllegalStateException(s"LocalData doesn't fulfil the contract: ${out.name} is missing")
      }
    }.toMap
    PredictResponse(rowTensors)
  }

  def tensorToLocalColumn(name: String, tensorProto: TensorProto): LocalDataColumn[_] = {
    val verified = verifyShape(tensorProto)
    val shaper = ColumnShaper(name, TensorShape(verified.tensorShape))
    val tensor = TypedTensorFactory.create(verified)
    shaper.shape(tensor.data)
  }
}
