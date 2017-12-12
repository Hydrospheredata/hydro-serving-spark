lazy val sparkVersion = scala.util.Properties.propOrElse("sparkVersion", "2.1.2")
lazy val sparkVersionLogger = taskKey[Unit]("Logs Spark version")

sparkVersionLogger := {
  val log = streams.value.log
  log.info(s"Spark version: $sparkVersion")
}
lazy val localSparkVersion = sparkVersion.substring(0,sparkVersion.lastIndexOf(".")).replace('.', '_')


lazy val sparkDependencies =
  Seq(
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "io.hydrosphere" %% s"spark-ml-serving-$localSparkVersion" % "0.2.1"
  )

lazy val akkaDependencies = {
  val akkaV = "2.4.14"
  val akkaHttpV = "10.0.0"
  Seq(
    "io.spray" %%  "spray-json" % "1.3.3"
  )
}

val protos = project.in(file("hydro-serving-protos"))

val root = project.in(file("."))
  .dependsOn(protos)
  .settings(
    name := s"spark-grpc-${localSparkVersion.replace('_', '.')}",
    organization := "io.hydrosphere",
    version := "0.1",
    scalaVersion := "2.11.11",

    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),

    libraryDependencies ++= akkaDependencies,
    libraryDependencies ++= sparkDependencies,

    libraryDependencies ++= Seq(
      "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
      "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
      "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,

      "org.tensorflow" % "proto" % "1.4.0",
      "org.scalactic" %% "scalactic" % "3.0.4",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    ),
    assemblyMergeStrategy in assembly := {
      case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
      case PathList("META-INF", "services", "org.apache.hadoop.fs.FileSystem") => MergeStrategy.filterDistinctLines
      case m if m.startsWith("META-INF") => MergeStrategy.discard
      case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
      case PathList("org", "apache", xs@_*) => MergeStrategy.first
      case PathList("org", "jboss", xs@_*) => MergeStrategy.first
      case "about.html" => MergeStrategy.rename
      case "reference.conf" => MergeStrategy.concat
      case PathList("org", "datanucleus", xs@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    test in assembly := {},

    assembly := {assembly.dependsOn(sparkVersionLogger).value}
  )
