
enablePlugins(DockerPlugin)

lazy val sparkVersion = scala.util.Properties.propOrElse("sparkVersion", "2.1.2")
lazy val sparkVersionLogger = taskKey[Unit]("Logs Spark version")

sparkVersionLogger := {
  val log = streams.value.log
  log.info(s"Spark version: $sparkVersion")
}
lazy val localSparkVersion = sparkVersion.substring(0,sparkVersion.lastIndexOf(".")).replace('.', '_')

name := s"hydro-serving-spark-${localSparkVersion.replace('_', '.')}"
organization := "io.hydrosphere"
version := IO.read(file("version")).trim
scalaVersion := "2.11.11"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Dependencies.all(sparkVersion, localSparkVersion)

compile in Compile := {(compile in Compile).dependsOn(sparkVersionLogger).value}

dockerfile in docker := {
  val dockerFilesLocation = baseDirectory.value / "src/main/docker/"
  val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
  val classpath = (dependencyClasspath in Compile).value
  val artifactTargetPath = s"/app/app.jar"

  new Dockerfile {
    from("anapsix/alpine-java:8")

    env("SIDECAR_PORT", "8080")
    env("SIDECAR_HOST","localhost")
    env("APP_PORT", "9090")

    label("DEPLOYMENT_TYPE", "APP")

    add(dockerFilesLocation, "/app/")
    add(classpath.files, "/app/lib/")
    add(jarFile, artifactTargetPath)

    volume("/model")

    cmd("/app/start.sh")
  }
}

imageNames in docker := Seq(
  ImageName(
    namespace = Some("hydrosphere"),
    repository = s"serving-runtime-spark-${sparkVersion}",
    tag = Some(s"${version.value}")
  )
)