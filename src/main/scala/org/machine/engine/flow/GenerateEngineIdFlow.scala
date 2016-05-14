package org.machine.engine.flow

import org.machine.engine.graph.Neo4JHelper

object GenerateEngineIdFlow{
  def enrichWithUUID(message:ClientMessage):EngineCapsule = {
    return new EngineCapsuleBase(
      Seq("addUUID"),
      Map[String, Any](),
      EngineCapsuleStatuses.Ok,
      None,
      message,
      Neo4JHelper.uuid
    )
  }
}
