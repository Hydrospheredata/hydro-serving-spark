import java.nio.file.{Files, Paths}

import com.google.protobuf.ByteString
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.grpc_spark.InferenceServiceImpl
import org.scalatest.AsyncWordSpec

class PredictServiceSpec extends AsyncWordSpec {
  "PredictService" should {
    "infer simple Word2Vec request" in {
      val infImpl = new InferenceServiceImpl(
        "src/test/resources/word2vec/model",
        "src/test/resources/word2vec/contract/contract.protobin")
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
      infImpl.predict(req).map{result =>
        assert(result === resp)
      }
    }
  }
}
