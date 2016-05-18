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
  The engine shuts down the data base on it's shutdown command. The good news is
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

  FIXME: Cannot actually start the database here.
  I would guess it's a file lock issue. There is probably connetion pooling
  I can take advantage of.
  */
  import org.machine.engine.Engine
  import com.typesafe.config._
  def runEngine(capsule: EngineCapsule):EngineCapsule = {
    sys.addShutdownHook{
      Console.println("Shutting Down Worker Engine")
      Engine.getInstance.shutdown()
    }

    /*
    NOTE: Hardcoded command.
    For the moment, let's just get the database communicating.
    */
    val datasets = Engine.getInstance.datasets()
    return capsule.enrich("CmdResult", datasets, Some("run-engine-flow"))
  }
}
