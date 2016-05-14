package org.machine.engine.flow

sealed trait EngineCapsuleStatus{
  def name:String
}

object EngineCapsuleStatuses{
    case object Ok extends EngineCapsuleStatus{ val name = "Ok"}
    case object Error extends EngineCapsuleStatus{ val name = "Error"}    
}
