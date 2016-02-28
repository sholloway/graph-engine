package org.machine.engine.graph.commands

sealed trait EngineCommand{
  def cmd: String
}

object EngineCommands{
  case object DefineElement extends EngineCommand{ val cmd = "element_definition";}
  case object EditElementDefinition extends EngineCommand{ val cmd = "edit_element_definition";}
  case object EditElementPropertyDefinition extends EngineCommand{ val cmd = "edit_element_property_definition";}
  case object RemoveElementPropertyDefinition extends EngineCommand{ val cmd = "remove_element_property_definition"}
}
