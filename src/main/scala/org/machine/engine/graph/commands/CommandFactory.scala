package org.machine.engine.graph.commands

import scala.collection.JavaConversions._
import org.neo4j.graphdb._
import org.machine.engine.logger._

/*
TODO:
Split this out. Have creation command factory and query command factory.
The query commands currently do not have a shared ancestor.
They need to return a List[T].
*/
object CommandFactory{
  def build(command:EngineCommand,
    database:GraphDatabaseService,
    cmdScope:CommandScope,
    cmdOptions:GraphCommandOptions,
    logger:Logger):Neo4JCommand = {
    return command match {
      case EngineCommands.DefineElement =>
        new CreateElementDefintion(database, cmdScope, cmdOptions, logger)
      case EngineCommands.EditElementDefinition =>
        new EditElementDefintion(database, cmdScope, cmdOptions, logger)
      case EngineCommands.DeleteElementDefintion =>
        new DeleteElementDefintion(database, cmdScope, cmdOptions, logger)
      case EngineCommands.EditElementPropertyDefinition =>
        new EditElementPropertyDefinition(database, cmdScope, cmdOptions, logger)
      case EngineCommands.RemoveElementPropertyDefinition =>
        new RemoveElementPropertyDefinition(database, cmdScope, cmdOptions, logger)
      case EngineCommands.CreateDataSet =>
        new CreateDataSet(database, cmdScope, cmdOptions, logger)
      case EngineCommands.EditDataSet =>
        new EditDataSet(database, cmdScope, cmdOptions, logger)
      case EngineCommands.ProvisionElement =>
        new CreateElement(database, cmdScope, cmdOptions, logger)
      case EngineCommands.EditElement =>
        new EditElement(database, cmdScope, cmdOptions, logger)
    }
  }
}
