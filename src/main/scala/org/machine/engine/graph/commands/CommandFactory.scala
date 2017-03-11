package org.machine.engine.graph.commands

import scala.collection.JavaConversions._
import org.neo4j.graphdb._


/*
TODO: Remove this after all comands have defined rules.
This should be replaced with the Decision Tree.
*/
object CommandFactory{
  def build(command:EngineCommand,
    database:GraphDatabaseService,
    cmdScope:CommandScope,
    cmdOptions:GraphCommandOptions):InternalEngineCommand = {
    return command match {
      case EngineCommands.DefineElement => new CreateElementDefintion(database, cmdScope, cmdOptions)
      case EngineCommands.EditElementDefinition => new EditElementDefintion(database, cmdScope, cmdOptions)
      case EngineCommands.DeleteElementDefintion => new DeleteElementDefintion(database, cmdScope, cmdOptions)

      case EngineCommands.EditElementPropertyDefinition => new EditElementPropertyDefinition(database, cmdScope, cmdOptions)
      case EngineCommands.RemoveElementPropertyDefinition =>new RemoveElementPropertyDefinition(database, cmdScope, cmdOptions)

      case EngineCommands.CreateDataSet => new CreateDataSet(database, cmdScope, cmdOptions)
      case EngineCommands.EditDataSet => new EditDataSet(database, cmdScope, cmdOptions)

      case EngineCommands.ProvisionElement => new CreateElement(database, cmdScope, cmdOptions)
      case EngineCommands.EditElement => new EditElement(database, cmdScope, cmdOptions)
      case EngineCommands.DeleteElement => new DeleteElement(database, cmdScope, cmdOptions)
      case EngineCommands.RemoveElementField => new RemoveElementField(database, cmdScope, cmdOptions)

      case EngineCommands.AssociateElements => new AssociateElements(database, cmdScope, cmdOptions)
      case EngineCommands.EditAssociation => new EditAssociation(database, cmdScope, cmdOptions)
      case EngineCommands.DeleteAssociation => new DeleteAssociation(database, cmdScope, cmdOptions)
      case EngineCommands.RemoveAssociationField => new RemoveAssociationField(database, cmdScope, cmdOptions)

      case EngineCommands.CreateNewUser => new CreateNewUser(database, cmdScope, cmdOptions)

    }
  }
}
