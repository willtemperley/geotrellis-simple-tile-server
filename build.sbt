name := "geotrellis-simple-tile-server"
version := "0.1.0"
scalaVersion := "2.10.6"
crossScalaVersions := Seq("2.11.5", "2.10.6")
organization := "com.azavea"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

resolvers += Resolver.bintrayRepo("azavea", "geotrellis")

Revolver.settings

fork := true

libraryDependencies ++= Seq(
  "com.azavea.geotrellis" %% "geotrellis-spark"         % "1.0.0-SNAPSHOT",
  "com.azavea.geotrellis" %% "geotrellis-s3"            % "1.0.0-SNAPSHOT",
  "io.spray"              %% "spray-routing"            % "1.3.3",
  "io.spray"              %% "spray-caching"            % "1.3.3",
  "io.spray"              %% "spray-can"                % "1.3.3",
  "org.apache.spark"      %% "spark-core"               % "1.5.2",
  "org.apache.hadoop"     % "hadoop-client"           % "2.6.0",
  "org.apache.commons"    % "commons-io"               % "1.3.2",
  "com.typesafe.akka"     %% "akka-actor"               % "2.3.9",
  "org.scalatest"       %%  "scalatest"      % "2.2.0" % "test"
)

// When creating fat jar, remote some files with
// bad signatures and resolve conflicts by taking the first
// versions of shared packaged types.
assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
