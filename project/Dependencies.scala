import sbt._

object Dependencies {

  def spark(sparkVersion: String, localSparkVersion: String) =
    Seq(
      "org.apache.spark" %% "spark-mllib" % sparkVersion exclude("io.netty", "netty") exclude("io.netty", "netty-all"),
      "io.hydrosphere" %% s"spark-ml-serving-$localSparkVersion" % "0.3.3"
    )

  def cats = Seq(
    "org.typelevel" %% "cats-effect" % "1.2.0"
  )

  def akka = {
    Seq(
      "io.spray" %%  "spray-json" % "1.3.3"
    )
  }

  def grpc = {
    Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,

      "io.hydrosphere" %% "serving-grpc-scala" % "2.1.0-preview0"
    )
  }

  def test = {
    Seq(
      "org.scalactic" %% "scalactic" % "3.0.4",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    )
  }

  def all = spark(_: String, _: String) ++ akka ++ grpc ++ test ++ cats

}