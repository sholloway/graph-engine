package org.machine.engine.graph.commands

import com.typesafe.scalalogging._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.neo4j.graphdb._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.internal._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.nodes._

class EchoRequest(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("EchoRequest: Executing Command")
    Console.println(cmdOptions)
    val message = cmdOptions.option[String]("message")
    return InsertCmdResult(message)
  }
}
