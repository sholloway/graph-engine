package org.machine.engine.flow

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, BinaryMessage, TextMessage}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, MergePreferred, Partition, RunnableGraph, Source, Sink}

object GraphCmdWorkerFlow{

  /*
  The deserialization of the JSON or protobuf should happen externally to this flow.
  The EngineCapsule should provide a RequestMessage that can be used to derive
  the action to take.
  */
  def flow:Flow[EngineCapsule, EngineMessage, NotUsed] = {
    val graph = GraphDSL.create(){ implicit b: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val logInboundMsg     = b.add(Flow[EngineCapsule].map[EngineCapsule](log[EngineCapsule](_)))
      val runEngineCmd      = b.add(Flow.fromFunction[EngineCapsule, EngineCapsule](runEngine))
      val logResult         = b.add(Flow[EngineCapsule].map[EngineCapsule](log[EngineCapsule](_)))
      val capsuleToResponse = b.add(Flow.fromFunction[EngineCapsule, EngineMessage](TransformEngineCapsuleToEngineMessage.transfrom))

      logInboundMsg ~> runEngineCmd ~> logResult ~> capsuleToResponse

      FlowShape(logInboundMsg.in, capsuleToResponse.out)
    }.named("worker-pool")
    return Flow.fromGraph(graph);
  }

  def log[T](item: T):T = {
    Console.println(item)
    return item
  }

  /*
  FIXME: Database Management Bug
  The engine shuts down the database on it's shutdown command. The good news is
  ~shutdown needs to be called explicitly, however, it needs to be thought through
  how multiple workers can connect to the in memory database. Only one should attempt
  to shut down the db when the database is done.
  */
  /*
  NOTE: Passing Engine Parameters
  How should the worker pool know what database to connect to? The configuration
  file works for the runtime case, however the tests currently spin up dedicated
  databases per spec.

  Options:
  - Change the tests to all use the config file for their database configs.
    - There is a seperate config file for tests, so possibly could still use
      seperate db paths per test spec.
  - Use an implicit parameter.
  - Pass the configuration around in memory.
  */
  import com.typesafe.config._
  import org.machine.engine.Engine
  import org.machine.engine.flow.requests.RequestMessage
  import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult}
  import org.machine.engine.graph.decisions.{ActionTypes, EntityTypes, Filters}
  def runEngine(capsule: EngineCapsule):EngineCapsule = {
    val request = capsule.attributes("deserializedMsg").asInstanceOf[RequestMessage]

    val result:EngineCmdResult = Engine.getInstance
      .reset
      .setUser(request.userId)
      .setScope(CommandScopes.pickScope(request.scope))
      .setActionType(ActionTypes.pickAction(request.actionType))
      .setEntityType(EntityTypes.pickEntity(request.entityType))
      .setFilter(Filters.pickFilter(request.filter))
    .run

    // return new EngineMessageBase(
    //   capsule.id,
    //   EngineCapsuleStatuses.Ok.name,
    //   EngineMessageTypes.CmdResult.name,
    //   capsule.message.payload /*NOTE: This is just for the moment.*/
    // )
    return capsule.enrich("CmdResult", result, Some("run-engine-flow"))
  }
}
