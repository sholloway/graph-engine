package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

/*
Notes
UniqueKillSwitch & SharedKillSwitch are intended to be used externally
of the stream. This might be good to have linked to Main or somewhere
in a catch block.

What are the shutdown scenarios?
1) The cockpit sends a message (somehow) to gracefully shutdown.
2) The cockpit sends a message (somehow) to forcefully shutdown.
3) The engine has a critical error and must shutdown. (Examples?)

Typically, it would be impossible to make a web server reboot via a client request.
This is the type of thing that JMX might be used to handle in a typical
Java/Container based application.

What are the options for controlling how the JAR is spun up from
Electron? There might be  way to send a signal to the jar to invoke a
JVM shutdown which could be preferable to a Web Socket command.

Can information be sent to stdin for the engine from the cockpit
using Node.js child_process module?
*/
class Shutdown(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends SystemCommand with LazyLogging{
  import Neo4JHelper._

  def execute():SystemCmdResult = {
    logger.debug("DeleteElement: Executing Command")

    return SystemCmdResult()
  }
}
