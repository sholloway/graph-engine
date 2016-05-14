package org.machine.engine.flow

sealed trait EngineMessageType{
  def name:String
}

object EngineMessageTypes{
    case object Receipt extends EngineMessageType{ val name = "Receipt"}
    case object CmdResult extends EngineMessageType{ val name = "CmdResult"}
}
