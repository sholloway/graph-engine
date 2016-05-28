package org.machine.engine.flow

import org.machine.engine.flow.requests.RequestMessage

object DeserializeClientMessage{
  /*Note:
  This will be responsible for deserialzing the client's message.
  That means conversion based JSON or Protobuf.
  For right now, just pass the original text message.
  */
  def deserialize(capsule: EngineCapsule):EngineCapsule = {
    val serializedMsg = capsule.message.payload

    //Assume JSON for the moment.
    val requestMsg = RequestMessage.fromJSON(serializedMsg)

    return capsule.enrich("deserializedMsg", requestMsg, Some("deserializeRequest"))
  }
}
