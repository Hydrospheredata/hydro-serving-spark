package io.hydrosphere.serving.grpc_spark

import com.google.protobuf.ByteString
import io.grpc.netty.NettyChannelBuilder
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType

object Client extends App {
  val channel = NettyChannelBuilder.forAddress("127.0.0.1", 9090).usePlaintext(true).build()
  val client = PredictionServiceGrpc.blockingStub(channel)
  val req = PredictRequest(
    inputs = Map(
      "text" -> TensorProto(
        DataType.DT_STRING,
        Some(TensorShapeProto(
          List(
            TensorShapeProto.Dim(-1)
          )
        )),
        stringVal = Seq(ByteString.copyFromUtf8("case"), ByteString.copyFromUtf8("class"), ByteString.copyFromUtf8("java"))
      )
    )
  )
  val resp = PredictResponse(
    Map(
      "result" -> TensorProto(
        DataType.DT_DOUBLE,
        Some(TensorShapeProto(
          List(
            TensorShapeProto.Dim(3)
          )
        )),
        doubleVal = List(0.05097582439581553, 0.020204303165276844, 0.02578992396593094)
      )
    )
  )
  val result = client.predict(req)
  println(result)
}
