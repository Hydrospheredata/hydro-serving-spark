package io.hydrosphere.serving.grpc_spark

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.api.predict.PredictResponse
import io.hydrosphere.serving.tensorflow.tensor.{TensorProto, TypedTensorFactory}
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.spark_ml_serving.common.{LocalData, LocalDataColumn}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

object TensorUtils {
  val logger = LoggerFactory.getLogger(TensorUtils.getClass)

  def requestToLocalData(dataFrame: Map[String, TensorProto], modelSignature: ModelSignature): LocalData = {
    val cols = modelSignature.inputs.map { in =>
      val tensorType = in.typeOrSubfields.dtype.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      val sigData = dataFrame(in.name)
      if (tensorType == sigData.dtype) {
        tensorToLocalColumn(in.name, sigData)
      } else {
        throw new IllegalArgumentException(s"Expected ${tensorType}, got ${sigData.dtype}")
      }
    }
    LocalData(cols.toList)
  }

  def localDataToResult(modelContract: ModelContract, localData: LocalData): PredictResponse = {
    logger.debug("{}", localData)
    val sig = modelContract.signatures.head
    val localMap = localData.toMapList
    val row = localMap.head
    val rowTensors = sig.outputs.map { out =>
      val outType = out.typeOrSubfields.dtype.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      localData.column(out.name) match {
        case Some(column) =>
          val colShape = column.data.length
          val tensorFactory = TypedTensorFactory(outType)
          val colData = column.data.flatMap{
            case s: Seq[_] => s
            case x => Seq(x)
          }
          val maybeColTensor = tensorFactory.createFromAny(colData, TensorShape.fromProto(out.shape))
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
    val shaper = ColumnShaper(name, tensorProto.tensorShape)
    val tensor = TypedTensorFactory.create(tensorProto)
    shaper(tensor.data)
  }

  case class ColumnShaper(name: String, tensorShapeProto: Option[TensorShapeProto]) {
    def apply(data: Seq[_]): LocalDataColumn[_] = {
      tensorShapeProto match {
        case Some(shape) =>
          val dims = shape.dim.map(_.size).reverseIterator
          LocalDataColumn[Any](name, List(shapeGrouped(data.toList, dims)))
        case None => LocalDataColumn[Any](name, data.toList) // as-is because None shape is a scalar
      }
    }

    @tailrec
    final def shapeGrouped(data: List[Any], shapeIter: Iterator[Long]): List[Any] = {
      if (shapeIter.nonEmpty) {
        val dimShape = shapeIter.next()
        if (dimShape == -1) {
          shapeGrouped(data, shapeIter)
        } else {
          shapeGrouped(data.grouped(dimShape.toInt).toList, shapeIter)
        }
      } else {
        data
      }
    }
  }

}
