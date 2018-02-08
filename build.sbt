
enablePlugins(DockerPlugin)

lazy val sparkVersion = scala.util.Properties.propOrElse("sparkVersion", "2.1.2")
lazy val isRelease = scala.util.Properties.propOrNone("isRelease").exists(_.toBoolean)
lazy val sparkVersionLogger = taskKey[Unit]("Logs Spark version")

sparkVersionLogger := {
  val log = streams.value.log
  log.info(s"Spark version: $sparkVersion")
}
lazy val localSparkVersion = sparkVersion.substring(0,sparkVersion.lastIndexOf(".")).replace('.', '_')

name := s"hydro-serving-spark-${localSparkVersion.replace('_', '.')}"
organization := "io.hydrosphere"
version := IO.read(file("version"))
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

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("anapsix/alpine-java:8")

    env("SIDECAR_PORT", "8080")
    env("SIDECAR_HOST","localhost")
    env("APP_PORT", "9090")

    add(artifact, artifactTargetPath)

    workDir("/app")

    run("apk", "update")
    run("apk", "add", "--no-cache", "curl")
    run("rm", "-rf", "/var/cache/apk/*")

    cmdRaw(s"java -jar ${artifact.name}")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some("hydrosphere"),
    repository = s"serving-runtime-spark",
    tag = Some(s"${localSparkVersion.replace('_', '.')}-latest")
  )
)