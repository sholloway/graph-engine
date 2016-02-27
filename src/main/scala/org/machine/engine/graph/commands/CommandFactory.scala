package org.machine.engine.graph.commands

import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import org.neo4j.graphdb._
import org.machine.engine.logger._

object CommandFactory{
  def build(command:EngineCommand,
    database:GraphDatabaseService,
    cmdScope:CommandScope,
    commandOptions:Map[String, AnyRef],
    logger:Logger):Neo4JCommand = {
    return command match {
      case EngineCommands.DefineElement =>
        new CreateElementDefintion(database, cmdScope, commandOptions, logger)
      case EngineCommands.EditElementDefinition =>
        new EditElementDefintion(database, cmdScope, commandOptions, logger)
    }
  }
}
