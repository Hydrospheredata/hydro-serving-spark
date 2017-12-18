lazy val sparkVersion = scala.util.Properties.propOrElse("sparkVersion", "2.1.2")
lazy val sparkVersionLogger = taskKey[Unit]("Logs Spark version")

sparkVersionLogger := {
  val log = streams.value.log
  log.info(s"Spark version: $sparkVersion")
}
lazy val localSparkVersion = sparkVersion.substring(0,sparkVersion.lastIndexOf(".")).replace('.', '_')

name := s"spark-grpc-${localSparkVersion.replace('_', '.')}"
organization := "io.hydrosphere"
version := "0.1"
scalaVersion := "2.11.11"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Dependencies.spark(sparkVersion, localSparkVersion)
libraryDependencies ++= Dependencies.akka
libraryDependencies ++= Dependencies.grpc
libraryDependencies ++= Dependencies.protoMsg
libraryDependencies ++= Dependencies.test("3.0.4")

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
}
test in assembly := {}

assembly := {assembly.dependsOn(sparkVersionLogger).value}
