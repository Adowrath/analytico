lazy val commonSettings = Seq(
  organization := "Star-TV",
  description := "Analytico - Analytics for you",
  version := "0.1",
  scalaVersion := "2.12.4",
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "utf8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Xfuture",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused",
    "-Yrangepos"
  ),
  scalacOptions in Compile in doc ++= Seq(
    "-groups",
    "-implicits",
    "-deprecation",
    "-encoding", "utf8",
    "-help",
    "-author",
    "-doc-title", description.value,
    "-doc-version", version.value,
    "-sourcepath", (baseDirectory.value / "src" / "main" / "scala").getAbsolutePath
  ),
  autoAPIMappings in Compile in doc := true,

  /**
    * Das Compiler-Plugin muss auf beiden Versionen aktiviert sein.
    */
  addCompilerPlugin(paradiseDependency)
)

lazy val paradiseDependency = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full

lazy val rootDependencies = Seq(
  "org.apache.poi" % "poi" % "3.17",
  "org.apache.poi" % "poi-ooxml" % "3.17",
  "com.google.apis" % "google-api-services-youtube" % "v3-rev182-1.22.0",
  "com.google.apis" % "google-api-services-youtubeAnalytics" % "v1-rev63-1.22.0",
  "com.google.apis" % "google-api-services-youtubereporting" % "v1-rev10-1.22.0",
  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.4",
  "com.google.http-client" % "google-http-client-jackson2" % "1.20.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.20.0",
  "com.google.collections" % "google-collections" % "1.0"
)

lazy val root = (project in file("."))
  .aggregate(analytico, macros)

lazy val macros = (project in file("macros"))
  .settings(
    name := "analytico-macros",
    commonSettings,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val analytico = (project in file("analytico"))
  .settings(
    name := "analytico",
    commonSettings,
    libraryDependencies ++= rootDependencies
  )
  .dependsOn(macros)
