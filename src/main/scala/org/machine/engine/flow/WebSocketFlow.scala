package org.machine.engine.flow

import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, BinaryMessage, TextMessage}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, MergePreferred, Partition, RunnableGraph, Source, Sink}

object WebSocketFlow{
  def flow:Flow[Message, Message, _] = {
    val graph = GraphDSL.create(){ implicit b: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val deadend = b.add(Sink.ignore)
      // val logMsg = b.add(Flow[Message].map[Message](m => {println(m); m}))
      val partitionByMsgType = b.add(Partition[Message](2, msg => partition(msg)))

      val msgToClientMsg    = b.add(Flow.fromFunction[Message, ClientMessage](transform))
      // val clientMsgToEngMsg = b.add(Flow.fromFunction[ClientMessage, EngineMessage](transform2))
      val coreFlow          = b.add(CoreFlow.flow)
      val engMsgToTxtMsg    = b.add(Flow.fromFunction[EngineMessage, Message](transform3))

      partitionByMsgType.out(0) ~> msgToClientMsg ~> coreFlow ~> engMsgToTxtMsg
      partitionByMsgType.out(1) ~> deadend

      FlowShape(partitionByMsgType.in, engMsgToTxtMsg.out)
    }.named("ws-flow")
    return Flow.fromGraph(graph);
  }

  def partition(message: Message):Int = {
    return message match{
      case t: TextMessage => 0
      case b: BinaryMessage => 1
    }
  }

  def transform(message: Message):ClientMessage = {
    val msg = message.asInstanceOf[TextMessage.Strict].text
    return new ClientMessageBase(msg)
  }

  def transform2(msg: ClientMessage):EngineMessage = {
    return new EngineMessageBase(
      "hard coded ID",
      EngineCapsuleStatuses.Ok.name,
      EngineMessageTypes.CmdResult.name,
      msg.payload
    )
  }

  def transform3(msg: EngineMessage):TextMessage = {
    val json = EngineMessage.toJSON(msg)
    return TextMessage.apply(json)
  }
}
