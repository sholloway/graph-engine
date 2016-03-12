package org.machine.engine.graph.commands

trait Neo4JQueryCommand[T]{
  def execute():List[T]
}
