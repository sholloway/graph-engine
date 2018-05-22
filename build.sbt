lazy val projectSettings = Seq(
	name := "machine-engine",
	version := "0.1.0-SNAPSHOT",
	scalaVersion := "2.11.8"
)

scalacOptions ++= Seq("" +
  "-unchecked",
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
	"-language","postfixOps")


import sbtassembly.AssemblyPlugin._

lazy val sbtAssemblySettings = Seq(
	mainClass in assembly := Some("org.machine.engine.Main"),
	assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
	assemblyJarName in assembly := s"${name.value}"
)

//skip tests during assembly process

assemblyMergeStrategy in assembly := {
	case PathList("META-INF", "LICENSES.txt")  => MergeStrategy.discard
	case x =>
		val oldStrategy = (assemblyMergeStrategy in assembly).value
		oldStrategy(x)
}

//Tests must be run sequentially due to neo4j interaction.
parallelExecution in Test := false

resolvers += "SBT Plugins" at "https://dl.bintray.com/sbt/sbt-plugin-releases"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.bintrayRepo("commercetools", "maven") //For sphere-json

//Load everything and set project settings
lazy val root = (project in file(".")).
	settings(projectSettings: _*).
  enablePlugins(AssemblyPlugin)

val AKKA_VERSION = "2.5.4"
val AKKA_HTTP_VERSION = "10.0.10"
//Maven Format: groupID % artifactID % revision
libraryDependencies ++= Seq(
	"org.scalatest" % "scalatest_2.11" % "2.2.6" % Test,
	"org.easymock" % "easymock" % "3.3.1" % Test,
	 "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION,
	 "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % Test,
	 "com.typesafe.akka" %% "akka-slf4j" % AKKA_VERSION,
	 "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION,
	 "com.typesafe.akka" %% "akka-http-core" % AKKA_HTTP_VERSION,
	 "com.typesafe.akka" %% "akka-http" % AKKA_HTTP_VERSION,
	 "com.typesafe.akka" %% "akka-http-testkit" % AKKA_HTTP_VERSION % Test,
	 "com.typesafe.akka" %% "akka-http-spray-json" % AKKA_HTTP_VERSION,
	 "com.typesafe.akka" %% "akka-camel" % AKKA_VERSION,
	 "org.apache.camel" % "camel-stream" % "2.18.2",
	 "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
	 "ch.qos.logback" % "logback-classic" % "1.1.7",
	 "com.typesafe" % "config" % "1.3.1",
	 "org.neo4j" % "neo4j" % "3.1.1",
	 "org.neo4j" % "neo4j-slf4j" % "3.1.1",
	 "com.softwaremill.akka-http-session" %% "core" % "0.5.1",
   "com.softwaremill.akka-http-session" %% "jwt"  % "0.5.1",
	 "org.neo4j" % "neo4j-graphviz" % "3.1.1" % Test,
	 "org.neo4j" % "neo4j-io" % "3.1.1" % Test,
	 "net.liftweb" %% "lift-json" % "3.2.0-M1", //3.0-RC3
	 "com.squareup.okhttp3" % "okhttp" % "3.6.0"% Test
)

//Split the project and dependencies.
//https://github.com/sbt/sbt-assembly#splitting-your-project-and-deps-jars
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = false)

lazy val displayCoverage = taskKey[Unit]("blah...")
displayCoverage := {
	import sys.process._
	"open target/scala-2.11/scoverage-report/index.html" !
}

addCommandAlias("cov", ";clean;coverage;test;coverageReport;displayCoverage")

//possibly simplify dependency on protobuf.
//protoc --proto_path=src/main/protobuf --java_out=tmp/gen src/main/protobuf/InboundMessageEnvelope.proto

initialCommands in console := """
  |import org.machine.engine.server._
	|import org.machine.engine.client._
	|import java.io.File;
	|import java.io.IOException;
	|import org.neo4j.graphdb._
	|import org.neo4j.graphdb.factory.GraphDatabaseFactory
	|import org.neo4j.io.fs.FileUtils
	|import java.nio.file.{Paths, Files}
	|import scala.collection.JavaConversions._
	|import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
	|import org.machine.engine.exceptions._
	|import org.machine.engine.graph._
	|import org.machine.engine.graph.Neo4JHelper._
	|import org.machine.engine.graph.commands._
	|import org.machine.engine.graph.nodes._
	|import org.machine.engine.graph.labels._
	|import org.machine.engine.graph.internal._
  |import com.softwaremill.session._
  """.stripMargin
