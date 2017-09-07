package org.machine.engine.graph.commands

sealed trait CommandScope{
  def scope: String
}

object CommandScopes{
  case object SystemScope extends CommandScope{ val scope = "internal_system";}
  case object SystemSpaceScope extends CommandScope{ val scope = "internal_system_space";}
  case object UserSpaceScope extends CommandScope{ val scope = "user";}
  case object DataSetScope extends CommandScope{ val scope = "data_set";}

  def pickScope(alias: String):CommandScope = {
    alias match{
      case "UserSpace"   => UserSpaceScope
      case "SystemSpace" => SystemSpaceScope
      case "System"      => SystemScope
      case "DataSet"     => DataSetScope
    }
  }

  def validScopes = Seq("UserSpace",
    "SystemSpace",
    "System",
    "DataSet")
}
