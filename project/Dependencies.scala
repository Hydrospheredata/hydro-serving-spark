import sbt._

object Dependencies {

  def spark(sparkVersion: String, localSparkVersion: String) =
    Seq(
      "org.apache.spark" %% "spark-mllib" % sparkVersion,
      "io.hydrosphere" %% s"spark-ml-serving-$localSparkVersion" % "0.2.1"
    )

  def akka = {
    Seq(
      "io.spray" %%  "spray-json" % "1.3.3"
    )
  }

  def grpc = {
    Seq(
      "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
      "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
      "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
    )
  }

  def protoMsg = {
    Seq(
      "io.hydrosphere" %% "serving-grpc-scala" % "0.0.11",
      "org.tensorflow" % "proto" % "1.4.0"
    )
  }

  def test(scalatestVersion: String) = {
    Seq(
      "org.scalactic" %% "scalactic" % scalatestVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % "test"
    )
  }

}
