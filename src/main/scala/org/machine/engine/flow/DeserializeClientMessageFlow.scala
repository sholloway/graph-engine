package org.machine.engine.flow

object DeserializeClientMessage{
  /*Note:
  This will be responsible for deserialzing the client's message.
  That means conversion based JSON or Protobuf.
  For right now, just pass the original text message.
  */
  def deserialize(capsule: EngineCapsule):EngineCapsule = {
    val deserializedMsg = capsule.message.payload

    //Assume JSON for the moment.    

    return capsule.enrich("deserializedMsg", deserializedMsg, Some("deserializeRequest"))
  }
}
