package org.machine.engine.graph.commands

trait InternalEngineCommand{
  def execute():EngineCmdResult
}

trait Neo4JQueryCommandB[T] extends InternalEngineCommand{
  def execute():QueryCmdResult[T]
}
