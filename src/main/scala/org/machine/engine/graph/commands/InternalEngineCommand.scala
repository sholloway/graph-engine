package org.machine.engine.graph.commands

trait InternalEngineCommand{
  def execute():EngineCmdResult
}

trait Neo4InsertCommand[T] extends InternalEngineCommand{
  import org.machine.engine.graph.Neo4JHelper._
  def execute():InsertCmdResult[T]
  def generateId(cmdOptions: GraphCommandOptions) = {
    if (!cmdOptions.contains("mid")){
      cmdOptions.addOption("mid", uuid)
    }
  }
}

trait Neo4UpdateCommand[T] extends InternalEngineCommand{
  def execute():UpdateCmdResult[T]
}

trait Neo4DeleteCommand[T] extends InternalEngineCommand{
  def execute():DeleteCmdResult[T]
}

trait Neo4JQueryCommand[T] extends InternalEngineCommand{
  def execute():QueryCmdResult[T]
}

trait SystemCommand extends InternalEngineCommand{
  def execute():SystemCmdResult
}
