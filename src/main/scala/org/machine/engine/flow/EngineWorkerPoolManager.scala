package org.machine.engine.flow

import org.machine.engine.Engine
import org.machine.engine.flow.requests.RequestMessage
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult}
import org.machine.engine.graph.decisions.{ActionTypes, EntityTypes, Filters}
import org.machine.engine.graph.nodes._
import org.machine.engine.encoder.json.OutboundJSONSerializer

object EngineWorkerPoolManager{
  def execute(capsule: EngineCapsule):EngineMessage = {
    // Console.println(capsule.attributes)
    val request = capsule.attributes("deserializedMsg").asInstanceOf[RequestMessage]

    val result:EngineCmdResult = Engine.getInstance
      .reset
      .setUser(Some(request.user))
      .setScope(CommandScopes.pickScope(request.scope))
      .setActionType(ActionTypes.pickAction(request.actionType))
      .setEntityType(EntityTypes.pickEntity(request.entityType))
      .setFilter(Filters.pickFilter(request.filter))
    .run

    val responseStr = OutboundJSONSerializer.serialize(result)

    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Ok.name,
      EngineMessageTypes.CmdResult.name,
      responseStr /*NOTE: This is just for the moment.*/
    )
  }  
}
