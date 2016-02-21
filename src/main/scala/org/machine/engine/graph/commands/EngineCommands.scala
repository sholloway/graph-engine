package org.machine.engine.graph.commands

sealed trait EngineCommand{
  def cmd: String
}

object EngineCommands{
  case object DefineElement extends EngineCommand{ val cmd = "element_definition";}
}
