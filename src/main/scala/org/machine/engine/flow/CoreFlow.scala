package org.machine.engine.flow

import akka.NotUsed
import akka.actor.ActorSystem
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, MergePreferred, Partition, RunnableGraph, Source, Sink}

object CoreFlow{
  def flow:Flow[ClientMessage, EngineMessage, NotUsed] = {
    val graph = GraphDSL.create(){ implicit b: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      implicit val rng = new Random(123)

      // val logOks               = b.add(Flow[EngineCapsule].map[EngineCapsule](c => {println(c); c}))
      // val logErrors            = b.add(Flow[EngineCapsule].map[EngineCapsule](c => {println(c); c}))
      // val deadend              = b.add(Sink.ignore)

      val addUUID              = b.add(Flow.fromFunction[ClientMessage, EngineCapsule](GenerateEngineIdFlow.enrichWithUUID))
      val broadcastUUID        = b.add(Broadcast[EngineCapsule](2))
      val deserialize          = b.add(Flow.fromFunction[EngineCapsule, EngineCapsule](DeserializeClientMessage.deserialize))
      val validateClientMsgMap = b.add(Flow.fromFunction[EngineCapsule, EngineCapsule](ValidateClientMessage.validate))
      val partitionByStatus    = b.add(Partition[EngineCapsule](2, capsule => PartitionEngineCapsuleByStatus.partition(capsule)))
      val errAndReceiptMerge   = b.add(Merge[EngineCapsule](2))
      val capsuleToResponse    = b.add(Flow.fromFunction[EngineCapsule, EngineMessage](TransformEngineCapsuleToEngineMessage.transfrom))

      /*
      NOTE: executeCmd will be replaced with a custom balanced worker pool.
      The workers shall be instances of the flow: GraphCmdWorkerFlow
      */
      val executeCmd           = b.add(Flow.fromFunction[EngineCapsule, EngineMessage](EngineWorkerPoolManager.execute))
      val engineMsgMerge       = b.add(Merge[EngineMessage](2))

      /*
      Things that occure in parallel need to have the async function called on them.
      The things after broadcast for example...
      http://doc.akka.io/docs/akka/2.4.4/scala/stream/stream-parallelism.html

      TODO: Leverage async to make the broadcast branches be async.
      */
      addUUID ~> broadcastUUID.in
                 broadcastUUID.out(0) ~> deserialize ~> validateClientMsgMap ~> partitionByStatus.in
                                                                                partitionByStatus.out(0) ~> executeCmd ~> engineMsgMerge.in(0)
                                                                                partitionByStatus.out(1) ~> errAndReceiptMerge.in(0)
                 broadcastUUID.out(1) ~>                                                                    errAndReceiptMerge.in(1)
                                                                                                            errAndReceiptMerge.out ~> capsuleToResponse ~> engineMsgMerge.in(1)

      FlowShape(addUUID.in, engineMsgMerge.out)
    }.named("core-flow")
    return Flow.fromGraph(graph);
  }

}
