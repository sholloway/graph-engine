package org.machine.engine.flow

object EngineWorkerPoolManager{
  def execute(capsule: EngineCapsule):EngineMessage ={
    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Processed.name,
      capsule.message.payload /*NOTE: This is just for the moment.*/
    )
  }
}
