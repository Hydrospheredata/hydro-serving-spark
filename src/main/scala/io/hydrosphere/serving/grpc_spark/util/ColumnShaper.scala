package io.hydrosphere.serving.grpc_spark.util

import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import io.hydrosphere.spark_ml_serving.common.LocalDataColumn

sealed trait ColumnShaper {
  def name: String
  def shape(data: Seq[Any]): LocalDataColumn[_]
}

case class ScalarShaper(name: String) extends ColumnShaper {
  override def shape(data: Seq[Any]): LocalDataColumn[_] = {
    LocalDataColumn(name, data.toList)
  }
}

case class DimShaper(name: String, dims: Seq[Long]) extends ColumnShaper {
  val strides: Seq[Long] = {
    val res = Array.fill(dims.length)(1L)
    val stLen = dims.length - 1
    for (i <- 0.until(stLen).reverse) {
      res(i) = res(i + 1) * dims(i + 1)
    }
    res.toSeq
  }

  def shape(data: Seq[Any]): LocalDataColumn[_] = {
    def shapeGrouped(dataId: Int, shapeId: Int): Any = {
      if (shapeId >= dims.length) {
        data(dataId)
      } else {
        val n = dims(shapeId).toInt
        val stride = strides(shapeId).toInt
        var mDataId = dataId
        val res = new Array[Any](n)

        for (i <- 0.until(n)) {
          val item = shapeGrouped(mDataId, shapeId + 1)
          res(i) = item
          mDataId += stride
        }
        Seq(res.toList)
      }
    } // def shapeGrouped

    LocalDataColumn(name, shapeGrouped(0, 0).asInstanceOf[List[Any]])
  }
}

object ColumnShaper {
  def apply(name: String, tensorShape: TensorShape): ColumnShaper = {
    tensorShape match {
      case AnyDims() => ScalarShaper(name)
      case Dims(dims, _) if dims.isEmpty => ScalarShaper(name)
      case Dims(dims, _) => DimShaper(name, dims)
    }
  }
}