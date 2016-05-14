package org.machine.engine.flow

object TransformEngineCapsuleToEngineMessage{
  def transfrom(capsule: EngineCapsule):EngineMessage = {
    return new EngineMessageBase(
      capsule.id,
      capsule.status.name,
      capsule.message.payload /*NOTE: This is just for the moment.*/
    )
  }
}
