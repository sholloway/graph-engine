package org.machine.engine.graph.commands

trait InternalEngineCommand{
  def execute():EngineCmdResult
}

trait Neo4InsertCommand[T] extends InternalEngineCommand{
  def execute():InsertCmdResult[T]
}

trait Neo4UpdateCommand[T] extends InternalEngineCommand{
  def execute():UpdateCmdResult[T]
}

trait Neo4DeleteCommand[T] extends InternalEngineCommand{
  def execute():DeleteCmdResult[T]
}

trait Neo4JQueryCommandB[T] extends InternalEngineCommand{
  def execute():QueryCmdResult[T]
}
