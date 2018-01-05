lazy val commonSettings = Seq(
  organization := "com.github.adowrath",
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
    "-Yrangepos",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-extra-implicit",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused:_",
    "-Ywarn-unused-import",
    "-Ywarn-value-discard"
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
  publishMavenStyle := true,
  libraryDependencies := rootDependencies,
  publishArtifact := false,

  /**
    * Das Compiler-Plugin muss sowohl bei der Makro-Definition als auch beim Expander vorhanden sein.
    */
  addCompilerPlugin(paradiseDependency),

  /**
    * Cross-Building? Warum nicht!
    */
  crossScalaVersions := Seq("2.11.12", "2.12.4", "2.13.0-M2"),

  /**
    * Damit IntelliJ einfacher eigene Rebuilds machen kann.
    * https://github.com/JetBrains/sbt-ide-settings/tree/750b993453fb3d1f31f371968d06e3fc792870a1#using-the-settings-without-plugin
    */
  SettingKey[Option[File]]("ide-output-directory") in Compile := Option(baseDirectory.value / "target" / "idea" / "classes"),
  SettingKey[Option[File]]("ide-output-directory") in Test := Option(baseDirectory.value / "target" / "idea" / "test-classes")
)

lazy val paradiseDependency = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full

lazy val miscDependencies = Seq(
  /**
    * Für bessere Datumsmanipulation.
    */
  "org.threeten" % "threeten-extra" % "1.2"
)

lazy val excelDependencies = Seq(
  "org.apache.poi" % "poi" % "3.17",
  "org.apache.poi" % "poi-ooxml" % "3.17"
)

lazy val youtubeApis = Seq(
  "com.google.apis" % "google-api-services-youtube" % "v3-rev182-1.22.0",
  "com.google.apis" % "google-api-services-youtubeAnalytics" % "v1-rev63-1.22.0",
  "com.google.apis" % "google-api-services-youtubereporting" % "v1-rev10-1.22.0",
  "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.4",
  "com.google.http-client" % "google-http-client-jackson2" % "1.22.0",
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
  "com.google.collections" % "google-collections" % "1.0"
)

lazy val rootDependencies =
  miscDependencies ++ excelDependencies ++ youtubeApis

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "analytico-root",
    run in Compile := {
      run in Compile in analytico
    }.evaluated
  )
  .aggregate(analytico, macros)

lazy val macros = (project in file("macros"))
  .settings(
    commonSettings,
    name := "analytico-macros",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val analytico = (project in file("analytico"))
  .settings(
    commonSettings,
    name := "analytico"
  )
  .dependsOn(macros)
