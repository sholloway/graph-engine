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
  import scala.reflect.{ClassTag, classTag}
  import scala.reflect.runtime.universe._
  private def serializeResponse[T: ClassTag](response: EngineCmdResult):String = {
    response match {
      case query: QueryCmdResult[T @unchecked] => matchQueryResultType(query.results)
      case _ => response.toString
    }
  }

  private def matchQueryResultType[T: ClassTag](result: Seq[T]):String = result match{
    case ds: Seq[DataSet @unchecked] if classTag[T] == classTag[DataSet] => serializeDataSetSeq(ds)
    case _ => result.toString
  }

  private def serializeDataSetSeq(results: Seq[DataSet]): String = {
    val json =
      ("datasets" ->
        results.map{ ds =>
          (
            ("id" -> ds.id) ~
            ("name" -> ds.name) ~
            ("description" -> ds.description) ~
            ("creationTime" -> ds.creationTime) ~
            ("lastModifiedTime" -> ds.lastModifiedTime)
          )
        }
      )
    return prettyRender(json)
  }
}
