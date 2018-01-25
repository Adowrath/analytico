lazy val commonSettings = Seq(
  organization := "com.github.adowrath",
  description := "Analytico - Analytics for you",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.4",
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "utf8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-explaintypes",

    "-Xfuture",

    "-Xlog-free-terms",
    "-Xlog-free-types",
    //"-Xlog-implicits",

    "-Xverify",
    "-Xlint:_",

    "-Yno-adapted-args",
    "-Yrangepos",

    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-Ywarn-value-discard"
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      /** Auf Scala 11 und darunter ist der Flag extra-implicit nicht vorhanden. */
      case Some((2, major)) if major >= 12 ⇒
        Seq(
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,-implicits,-patvars",
          "-opt:l:method",
          "-opt-warnings:_")
      case _ ⇒
        Seq("-Ywarn-unused")
    }
  },
  Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-implicits",
    "-deprecation",
    "-encoding", "utf8",
    "-help",
    "-author",
    "-diagrams",
    "-diagrams-max-implicits", "5",
    "-doc-title", description.value,
    "-doc-version", version.value,
    "-sourcepath", (baseDirectory.value / "src" / "main" / "scala").getAbsolutePath
  ),
  Compile / doc / autoAPIMappings := true,
  publishMavenStyle := true,
  libraryDependencies ++= rootDependencies,
  publishArtifact := false,

  /**
    * Das Compiler-Plugin muss sowohl bei der Makro-Definition als auch beim Expander vorhanden sein.
    */
  addCompilerPlugin(paradiseDependency),

  /**
    * Damit IntelliJ einfacher eigene Rebuilds machen kann.
    * https://github.com/JetBrains/sbt-ide-settings/tree/750b993453fb3d1f31f371968d06e3fc792870a1#using-the-settings-without-plugin
    */
  Compile / SettingKey[Option[File]]("ide-output-directory") := Some(baseDirectory.value / "target" / "idea" /      "classes"),
  Test    / SettingKey[Option[File]]("ide-output-directory") := Some(baseDirectory.value / "target" / "idea" / "test-classes"),
  fork := true,

  wartremoverWarnings ++= {
    import Wart._
    Warts.allBut(
      Any,
      AsInstanceOf,
      DefaultArguments,
      ImplicitParameter,
      MutableDataStructures,
      NonUnitStatements,
      Nothing,
      Overloading,
      Option2Iterable,
      Recursion,
      TraversableOps,
      Var
    )
  }
)

lazy val paradiseDependency = "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full

lazy val miscDependencies = Seq(
  /**
    * Für bessere Datumsmanipulation.
    */
  "org.threeten" % "threeten-extra" % "1.2",

  /**
    * ScalaFX!
    *
    * @see [[http://www.scalafx.org/ ScalaFX]]
    */
  "org.scalafx" %% "scalafx" % "8.0.144-R12",

  /**
    * FXML-Schnittstelle für Scala.
    */
  "org.scalafx" %% "scalafxml-core-sfx8" % "0.4",

  /**
    * Circe is a JSON-serializer.
    */
  "io.circe" %% "circe-core" % "0.9.0",
  "io.circe" %% "circe-generic" % "0.9.0",
  "io.circe" %% "circe-parser" % "0.9.0",

  /**
    * ScalaTest dependencies.
    */
  "org.scalactic" %% "scalactic" % "3.0.4",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
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
    Compile / run := (analytico / Compile / run).evaluated
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
