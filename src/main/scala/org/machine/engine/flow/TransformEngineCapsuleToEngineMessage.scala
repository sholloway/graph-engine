package org.machine.engine.flow

object TransformEngineCapsuleToEngineMessage{
  /*
  At the moment, this should only be getting traffic for
  Receipts and Errors from client validation.
  */
  def transfrom(capsule: EngineCapsule):EngineMessage = {
    val msgType = if(capsule.status == EngineCapsuleStatuses.Ok) EngineMessageTypes.Receipt else EngineMessageTypes.CmdResult
    return new EngineMessageBase(
      capsule.id,
      capsule.status.name,
      msgType.name,
      capsule.message.payload 
    )
  }
}
