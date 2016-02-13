lazy val projectSettings = Seq(
	name := "machine-engine",
	version := "0.0.1",
	scalaVersion := "2.10.4"
)

import sbtassembly.AssemblyPlugin._

lazy val sbtAssemblySettings = Seq(
	mainClass in assembly := Some("org.machine.engine.Main"),
	assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript)),
	assemblyJarName in assembly := s"${name.value}"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
libraryDependencies += "com.eed3si9n" % "sbt-assembly" % "0.14.1" from "http://dl.bintray.com/sbt/sbt-plugin-releases/com.eed3si9n/sbt-assembly/scala_2.10/sbt_0.13/0.14.1/jars/sbt-assembly.jar"

//Load everything and set project settings
lazy val root = (project in file(".")).
	settings(projectSettings: _*)
	// settings(sbtAssemblySettings: _*)

//Maven Format: groupID % artifactID % revision
libraryDependencies ++= Seq(
	"org.scalatest" % "scalatest_2.11" % "2.2.4" % Test,
	"org.easymock" % "easymock" % "3.3.1" % Test,
	 "com.typesafe.akka" %% "akka-actor" % "2.3.9",
	 "org.zeromq" % "jeromq" % "0.3.5",
   //http://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
   "com.google.protobuf" % "protobuf-java" % "3.0.0-beta-1"
)

initialCommands in console := """
  |import org.machine.engine.server._
	|import org.machine.engine.client._
	""".stripMargin
