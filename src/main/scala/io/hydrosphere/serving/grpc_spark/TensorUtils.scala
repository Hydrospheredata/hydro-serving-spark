package io.hydrosphere.serving.grpc_spark

import com.google.protobuf.ByteString
import hydrosphere.contract.model_contract.ModelContract
import hydrosphere.contract.model_signature.ModelSignature
import hydrosphere.tensorflow.serving.predict.PredictResponse
import hydrosphere.tensorflow.tensor.TensorProto
import hydrosphere.tensorflow.tensor_shape.TensorShapeProto
import hydrosphere.tensorflow.types.DataType
import hydrosphere.tensorflow.types.DataType.{DT_BOOL, DT_COMPLEX128, DT_COMPLEX64, DT_DOUBLE, DT_FLOAT, DT_INT16, DT_INT32, DT_INT64, DT_INT8, DT_INVALID, DT_QINT16, DT_QINT32, DT_QINT8, DT_QUINT16, DT_QUINT8, DT_STRING, DT_UINT16, DT_UINT32, DT_UINT64, DT_UINT8, DT_VARIANT, Unrecognized}
import io.hydrosphere.spark_ml_serving.common.{LocalData, LocalDataColumn}

object TensorUtils {
  def areShapesCompatible(
    contractShape: Option[TensorShapeProto],
    dataShape: Option[TensorShapeProto]
  ): Boolean = {
    contractShape -> dataShape match {
      case (data, contract) if data == contract => true // two identical tensors
      case (None, Some(_)) => false // scalar and non-scalar
      case (Some(_), None) => false // non-scalar and scalar
      case (Some(contract), Some(data)) if contract.dim.length != data.dim.length => false // different dims
      case (Some(contract), Some(data)) =>
        contract.dim.map(_.size).zip(data.dim.map(_.size)).forall {
          case (requiredLen, actualLen) =>
            if (requiredLen == actualLen) {
              true // N == M
            } else if (requiredLen == -1) {
              true // -1 == M
            } else {
              false
            }
        }
    }
  }

  def requestToLocalData(dataFrame: Map[String, TensorProto], modelSignature: ModelSignature): LocalData = {
    val cols = modelSignature.inputs.map { in =>
      val tensorData = in.infoOrDict.info.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      val sigData = dataFrame(in.fieldName)
      if (tensorData.dtype == sigData.dtype) {
        tensorToLocalColumn(in.fieldName, sigData)
      } else {
        throw new IllegalArgumentException(s"Expected ${tensorData.dtype}, got ${sigData.dtype}")
      }
    }
    LocalData(cols.toList)
  }

  def localDataToResult(modelContract: ModelContract, localData: LocalData): PredictResponse = {
    println(localData)
    val sig = modelContract.signatures.head
    val localMap = localData.toMapList
    val row = localMap.head
    val rowTensors = sig.outputs.map { out =>
      val outTensor = out.infoOrDict.info.getOrElse(throw new IllegalStateException("Runtime doesnt support nested contracts"))
      val outData = row(out.fieldName)
      out.fieldName -> anyToTensor(outData, outTensor.dtype, outTensor.tensorShape)
    }.toMap
    PredictResponse(rowTensors)
  }

  def anyToTensor(data: Any, dtype: DataType, shape: Option[TensorShapeProto]): TensorProto = {
    def flatten(ls: Seq[Any]): Seq[Any] = ls flatMap {
      case ms: Seq[_] => flatten(ms)
      case e => List(e)
    }

    println(data)
    val reshapedData = shape match {
      case Some(_) => flatten(data.asInstanceOf[Seq[Any]])
      case None => List(data)
    }

    val tensor = TensorProto(dtype = dtype, tensorShape = shape)
    dtype match {
      case DT_INVALID => throw new IllegalArgumentException(s"can't convert data to DT_INVALID  has an invalid dtype")
      case DT_FLOAT => tensor.copy(floatVal = reshapedData.map(_.asInstanceOf[Float]))
      case DT_DOUBLE => tensor.copy(doubleVal = reshapedData.map(_.asInstanceOf[Double]))
      case DT_INT8 | DT_INT16 | DT_INT32 => tensor.copy(intVal = reshapedData.map(_.asInstanceOf[Int]))
      case DT_UINT8 | DT_UINT16 | DT_UINT32 => tensor.copy(uint32Val = reshapedData.map(_.asInstanceOf[Int]))
      case DT_INT64 => tensor.copy(int64Val = reshapedData.map(_.asInstanceOf[Long]))
      case DT_UINT64 => tensor.copy(uint64Val = reshapedData.map(_.asInstanceOf[Long]))

      case DT_QINT8 | DT_QINT16 | DT_QINT32 => tensor.copy(intVal = reshapedData.map(_.asInstanceOf[Int]))
      case DT_QUINT8 | DT_QUINT16 => tensor.copy(uint32Val = reshapedData.map(_.asInstanceOf[Int]))
      case DT_COMPLEX64 => tensor.copy(scomplexVal = reshapedData.map(_.asInstanceOf[Float]))
      case DT_COMPLEX128 => tensor.copy(dcomplexVal = reshapedData.map(_.asInstanceOf[Double]))

      case DT_STRING => tensor.copy(stringVal = reshapedData.map(x => ByteString.copyFromUtf8(x.toString)))
      case DT_BOOL => tensor.copy(boolVal = reshapedData.map(_.asInstanceOf[Boolean]))
      case DT_VARIANT => throw new IllegalArgumentException(s"Cannot process DT_VARIANT Tensor. Not supported yet.")

      case Unrecognized(value) => throw new IllegalArgumentException(s"Cannot process Tensor with Unrecognized($value) dtype")
      case x => throw new IllegalArgumentException(s"Cannot process Tensor with $x dtype")// refs
    }
  }

  def tensorToLocalColumn(name: String, tensorProto: TensorProto): LocalDataColumn[_] = {
    val shaper = ColumnShaper(name, tensorProto.tensorShape)
    tensorProto.dtype match {
      case DT_INVALID => throw new IllegalArgumentException(s"$tensorProto has an invalid dtype")
      case DT_FLOAT => shaper(tensorProto.floatVal)
      case DT_DOUBLE => shaper(tensorProto.doubleVal)
      case DT_INT8 | DT_INT16 | DT_INT32 => shaper(tensorProto.intVal)
      case DT_UINT8 | DT_UINT16 | DT_UINT32 => shaper(tensorProto.uint32Val)
      case DT_INT64 => shaper(tensorProto.int64Val)
      case DT_UINT64 => shaper(tensorProto.uint64Val)

      case DT_QINT8 | DT_QINT16 | DT_QINT32 => shaper(tensorProto.intVal)
      case DT_QUINT8 | DT_QUINT16 => shaper(tensorProto.uint32Val)
      case DT_COMPLEX64 => shaper(tensorProto.scomplexVal)
      case DT_COMPLEX128 => shaper(tensorProto.dcomplexVal)

      case DT_STRING => shaper(tensorProto.stringVal.map(_.toStringUtf8()))
      case DT_BOOL => shaper(tensorProto.boolVal)
      case DT_VARIANT => throw new IllegalArgumentException(s"Cannot process DT_VARIANT Tensor. Not supported yet.")

      case Unrecognized(value) => throw new IllegalArgumentException(s"Cannot process Tensor with Unrecognized($value) dtype")
      case x => throw new IllegalArgumentException(s"Cannot process Tensor with $x dtype")// refs
    }
  }

  case class ColumnShaper(name: String, tensorShapeProto: Option[TensorShapeProto]) {
    def apply[T](data: Seq[T]): LocalDataColumn[_] = {
      tensorShapeProto match {
        case Some(shape) =>
          val dims = shape.dim.map(_.size).reverseIterator
          LocalDataColumn[Any](name, List(shapeGrouped(data.toList, dims)))
        case None => LocalDataColumn[Any](name, data.toList) // as-is because None shape is a scalar
      }
    }

    def shapeGrouped(data: List[Any], shapeIter: Iterator[Long]): List[Any] = {
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
