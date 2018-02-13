import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}
import io.hydrosphere.serving.grpc_spark.InferenceServiceImpl
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.AsyncWordSpec

import scala.concurrent.ExecutionContext

class PredictServiceSpec extends AsyncWordSpec {
  "PredictService" should {
    "infer simple Word2Vec request" in {
      val infImpl = new InferenceServiceImpl("src/test/resources/word2vec")
      val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
      val server = NettyServerBuilder.forPort(9091).addService(service).build()
      server.start()
      val channel = NettyChannelBuilder.forAddress("0.0.0.0", 9091).usePlaintext(true).build()
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
      server.shutdown()
      assert(result === resp)
    }

    "infer remote runtime" in {
      val channel = NettyChannelBuilder.forAddress("0.0.0.0", 9090).build()
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
      assert(result === resp)
    }
  }
}
