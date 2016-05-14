package org.machine.engine.flow

object EngineWorkerPoolManager{
  def execute(capsule: EngineCapsule):EngineMessage ={
    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Ok.name,
      EngineMessageTypes.CmdResult.name,
      capsule.message.payload /*NOTE: This is just for the moment.*/
    )
  }
}
