package org.machine.engine.flow

object PartitionEngineCapsuleByStatus{
  def partition(capsule: EngineCapsule):Int = {
    return capsule.status match {
      case EngineCapsuleStatuses.Ok => 0
      case EngineCapsuleStatuses.Error => 1
      case _ => 1
    }
  }
}
