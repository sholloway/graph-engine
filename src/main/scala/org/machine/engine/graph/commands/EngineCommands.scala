package org.machine.engine.graph.commands

sealed trait EngineCommand{
  def cmd: String
}

object EngineCommands{
  case object DefineElement extends EngineCommand{ val cmd = "create_element_definition";}
  case object EditElementDefinition extends EngineCommand{ val cmd = "edit_element_definition";}
  case object EditElementPropertyDefinition extends EngineCommand{ val cmd = "edit_element_property_definition";}
  case object RemoveElementPropertyDefinition extends EngineCommand{ val cmd = "remove_element_property_definition"}
  case object DeleteElementDefintion extends EngineCommand{ val cmd = "delete_element_definition"}
  case object CreateDataSet extends EngineCommand{ val cmd = "create_data_set"}
  case object EditDataSet extends EngineCommand{ val cmd = "edit_data_set"}
}
