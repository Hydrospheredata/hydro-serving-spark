
enablePlugins(DockerPlugin)

lazy val sidecarVersion = scala.util.Properties.propOrElse("sidecarVersion", "0.0.1")
lazy val sidecarPort = scala.util.Properties.propOrElse("sidecarPort", "8080")

lazy val appPort = scala.util.Properties.propOrElse("appPort", "9090")

lazy val sparkVersion = scala.util.Properties.propOrElse("sparkVersion", "2.1.2")
lazy val sparkVersionLogger = taskKey[Unit]("Logs Spark version")

sparkVersionLogger := {
  val log = streams.value.log
  log.info(s"Spark version: $sparkVersion")
}
lazy val localSparkVersion = sparkVersion.substring(0,sparkVersion.lastIndexOf(".")).replace('.', '_')

name := s"spark-grpc-${localSparkVersion.replace('_', '.')}"
organization := "io.hydrosphere"
version := "0.0.1"
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

    env("SIDECAR_HTTP_PORT", sidecarPort)
    env("APP_HTTP_PORT", appPort)
    env("APP_START_SCRIPT", "/app/start.sh")
    env("SERVER_JAR", artifact.name)

    add(file("start.sh"), "/app/start.sh")
    addRaw(s"http://repo.hydrosphere.io/hydrosphere/static/hydro-serving-sidecar-install-$sidecarVersion.sh", "/app/sidecar.sh")
    add(artifact, artifactTargetPath)

    workDir("/app")

    run("apk", "update")
    run("apk", "add", "--no-cache", "curl")
    run("chmod", "+x", "/app/start.sh")
    run("chmod", "+x", "./sidecar.sh")
    run("./sidecar.sh", "--target", "/hydro-serving/sidecar", "--", "alpine")
    run("rm", "-rf", "sidecar.sh")
    run("rm", "-rf", "/var/cache/apk/*")

    healthCheck(s"--interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:$appPort/health || exit 1")

    cmd("/hydro-serving/sidecar/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some("hydrosphere"),
    repository = s"serving-grpc-runtime-spark-$localSparkVersion",
    tag = Some(version.value)
  )
)