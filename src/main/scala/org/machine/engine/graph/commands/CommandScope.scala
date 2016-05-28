package org.machine.engine.graph.commands

sealed trait CommandScope{
  def scope: String
}

object CommandScopes{
  case object SystemSpaceScope extends CommandScope{ val scope = "internal_system_space";}
  case object UserSpaceScope extends CommandScope{ val scope = "internal_user_space";}
  case object DataSetScope extends CommandScope{ val scope = "internal_data_set";}

  def pickScope(alias: String):CommandScope = {
    alias match{
      case "UserSpace"   => UserSpaceScope
      case "SystemSpace" => SystemSpaceScope
      case "DataSet"     => DataSetScope
    }
  }
}
