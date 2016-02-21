package org.machine.engine.graph.commands

sealed trait CommandScope{
  def scope: String
}

object CommandScopes{
  case object SystemSpaceScope extends CommandScope{ val scope = "internal_system_space";}
  case object UserSpaceScope extends CommandScope{ val scope = "internal_user_space";}
}
