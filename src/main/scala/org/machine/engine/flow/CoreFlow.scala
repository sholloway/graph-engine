package org.machine.engine.flow

import akka.NotUsed
import akka.actor.ActorSystem
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}

import org.machine.engine.graph.Neo4JHelper

object CoreFlow{
  def flow:Flow[CmdRequest, CmdResponse, NotUsed] = {
    val graph = GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val requestEnrichment:FlowShape[CmdRequest, CmdRequestEnrichment] = builder.add(Flow.fromFunction(enrichWithUUID))
      val reqToResponse:FlowShape[CmdRequestEnrichment, CmdResponse]    = builder.add(Flow.fromFunction(enrichedReqToResponse))
      val responseMerge                                                 = builder.add(Merge[CmdResponse](1))

      requestEnrichment ~> reqToResponse ~> responseMerge

      FlowShape(requestEnrichment.in, responseMerge.out)
    }.named("core-flow")
    return Flow.fromGraph(graph);
  }

  private def enrichWithUUID(item:CmdRequest):CmdRequestEnrichment = {
    return CmdRequestEnrichment(item, Neo4JHelper.uuid)
  }

  private def enrichedReqToResponse(request: CmdRequestEnrichment):CmdResponse ={
    return CmdResponse(request.request.payload, request.id)
  }

  case class CmdRequest(payload:String){
    override def toString():String = {
      s"Payload: $payload"
    }
  }

  case class CmdRequestEnrichment(request: CmdRequest, id: String){
    override def toString():String = {
      s"request: $request uuid: $id"
    }
  }

  case class CmdResponse(response: String, id: String){
    override def toString():String = {
      s"uuid: $id response: $response"
    }
  }
}
