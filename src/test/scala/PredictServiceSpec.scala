import com.google.protobuf.ByteString
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}
import io.hydrosphere.serving.grpc_spark.InferenceServiceImpl
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.tensor.{DoubleTensor, TensorProto}
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.AsyncWordSpec

import scala.concurrent.ExecutionContext

class PredictServiceSpec extends AsyncWordSpec {
  "PredictService" should {
    //    "generate contract for Word2Vec" in {
    //      val contract = ModelContract(
    //        modelName = "word2vec",
    //        signatures = Seq(
    //          ModelSignature(
    //            signatureName = "default_spark",
    //            inputs = Seq(
    //              ModelField(
    //                "text",
    //                TensorShape.vector(-1).toProto,
    //                typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_STRING)
    //              )
    //            ),
    //            outputs = Seq(
    //              ModelField(
    //                "result",
    //                TensorShape.vector(3).toProto,
    //                typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)
    //              )
    //            )
    //          )
    //        )
    //      )
    //      val contractPath = Paths.get("contract.protobin")
    //      Files.write(contractPath, contract.toByteArray)
    //      assert(Files.exists(contractPath))
    //    }
    //    "generate contract for Binarizer" in {
    //      val contract = ModelContract(
    //        "binarizer",
    //        Seq(ModelSignature(
    //          signatureName = "default_spark",
    //          inputs = Seq(ModelField(
    //            name = "feature",
    //            shape = TensorShape.scalar.toProto,
    //            typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)
    //          )),
    //          outputs = Seq(ModelField(
    //            name = "binarized_feature",
    //            shape = TensorShape.scalar.toProto,
    //            typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)
    //          ))
    //        ))
    //      )
    //      val contractPath = Paths.get("contract.protobin")
    //      Files.write(contractPath, contract.toByteArray)
    //      assert(Files.exists(contractPath))
    //    }

    "infer simple Word2Vec request" in {
      val infImpl = new InferenceServiceImpl("src/test/resources/word2vec")
      val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
      val server = NettyServerBuilder.forPort(9091).addService(service).build()
      server.start()
      val channel = NettyChannelBuilder.forAddress("0.0.0.0", 9091).usePlaintext().build()
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

    "infer simple Binarizer request" in {
      val infImpl = new InferenceServiceImpl("src/test/resources/binarizer")
      val service = PredictionServiceGrpc.bindService(infImpl, ExecutionContext.global)
      val server = NettyServerBuilder.forPort(9091).addService(service).build()
      server.start()
      val channel = NettyChannelBuilder.forAddress("0.0.0.0", 9091).usePlaintext().build()
      val client = PredictionServiceGrpc.blockingStub(channel)
      val req = PredictRequest(
        inputs = Map(
          "feature" -> DoubleTensor(TensorShape.scalar, Seq(5.0)).toProto
        )
      )
      val resp = PredictResponse(
        Map(
          "binarized_feature" -> DoubleTensor(TensorShape.scalar, Seq(0.0)).toProto
        )
      )
      val result = client.predict(req)
      server.shutdown()
      assert(result === resp)
    }
  }
}