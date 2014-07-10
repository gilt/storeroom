name := "storeroom"

organization in ThisBuild := "com.gilt"

scalaVersion in ThisBuild := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.1")

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

version in ThisBuild := "0.0.2-SNAPSHOT"

homepage in ThisBuild := Some(url("http://github.com/gilt/storeroom"))

licenses in GlobalScope += "MIT" -> url("https://github.com/gilt/storeroom/raw/master/LICENSE")

lazy val root = (project in file("."))
  .aggregate(core, dynamodb)
  .settings(
    publishArtifact := false
  )

lazy val core = project
  .in(file("core"))
  .settings(name := "storeroom-core")
  .settings(commonSettings: _*)

lazy val dynamodb = project
  .in(file("dynamodb"))
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "storeroom-dynamodb",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk" % "1.5.7"
    )
  )


lazy val commonSettings: Seq[Setting[_]] = instrumentSettings ++ Seq(
  ScoverageKeys.highlighting := true,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.2.0" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.4" % "test",
    "org.mockito" % "mockito-core" % "1.9.5" % "test"
  )
)

// scoverage
instrumentSettings

ScoverageKeys.highlighting := true
