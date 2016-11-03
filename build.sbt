// Use the packageServerless alias to build a serverless zip file

enablePlugins(JavaAppPackaging)
addCommandAlias("packageServerless", "universal:packageBin")

lazy val root = (project in file(".")).
  settings(
    name := "email-forwarder",
    version := "1.0",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
      "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
      "com.amazonaws" % "aws-java-sdk-ses" % "1.11.22",
      "io.circe" %% "circe-core" % "0.5.1",
      "io.circe" %% "circe-generic" % "0.5.1",
      "io.circe" %% "circe-parser" % "0.5.1",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"
    ),
    packageName in Universal := name.value,
    topLevelDirectory := None
  )
