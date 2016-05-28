package org.machine.engine.flow

import org.machine.engine.Engine
import org.machine.engine.flow.requests.RequestMessage
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult}
import org.machine.engine.graph.decisions.{ActionTypes, EntityTypes, Filters}
import org.machine.engine.graph.nodes._

object EngineWorkerPoolManager{
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._

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

    val responseStr = serializeResponse(result)

    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Ok.name,
      EngineMessageTypes.CmdResult.name,
      responseStr /*NOTE: This is just for the moment.*/
    )
  }

  /*TODO Convert to JSON*/
  private def serializeResponse(response: EngineCmdResult):String = {

    response match {
      case query: QueryCmdResult[DataSet] => serializeQuery(query)
      case _ => response.toString
    }

  }

  private def serializeQuery(query: QueryCmdResult[DataSet]): String = {
    val json =
      ("datasets" ->
        query.results.map{ ds =>
          (
            ("id" -> ds.id) ~
            ("name" -> ds.name) ~
            ("description" -> ds.description) ~
            ("creationTime" -> ds.creationTime) ~
            ("lastModifiedTime" -> ds.lastModifiedTime)
          )
        }
      )
    return pretty(render(json))
  }
}
