package org.machine.engine.graph.commands

sealed trait EngineCommand{
  def cmd: String
}

object EngineCommands{
  case object DefineElement extends EngineCommand{ val cmd = "create_element_definition";}
  case object EditElementDefinition extends EngineCommand{ val cmd = "edit_element_definition";}
  case object DeleteElementDefintion extends EngineCommand{ val cmd = "delete_element_definition"}

  case object EditElementPropertyDefinition extends EngineCommand{ val cmd = "edit_element_property_definition";}
  case object RemoveElementPropertyDefinition extends EngineCommand{ val cmd = "remove_element_property_definition"}

  case object CreateDataSet extends EngineCommand{ val cmd = "create_data_set"}
  case object EditDataSet extends EngineCommand{ val cmd = "edit_data_set"}

  case object ProvisionElement extends EngineCommand{ val cmd = "create_element"}
  case object EditElement extends EngineCommand{ val cmd = "edit_element"}
  case object DeleteElement extends EngineCommand{ val cmd = "delete_element"}
  case object RemoveElementField extends EngineCommand{ val cmd = "remove_element_field"}

  case object AssociateElements extends EngineCommand{ val cmd = "associate_elements"}
  case object EditAssociation extends EngineCommand{ val cmd = "edit_association"}
  case object DeleteAssociation extends EngineCommand{ val cmd = "delete_association"}
  case object RemoveAssociationField extends EngineCommand{ val cmd = "remove_association_field"}
}
