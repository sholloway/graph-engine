package org.machine.engine.flow

import akka.NotUsed
import akka.actor.ActorSystem
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}

import org.machine.engine.graph.Neo4JHelper

object CoreFlow{
  def flow:Flow[ClientMessage, EngineMessage, NotUsed] = {
    val graph = GraphDSL.create(){ implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val addUUID = builder.add(Flow.fromFunction[ClientMessage, EngineCapsule](enrichWithUUID))
      // val broadcastUUID     = builder.add(Broadcast[EngineCapsule](2))
      // val log               = builder.add(Flow[EngineCapsule].map[Int](x => {println(x); x}))
      val capsuleToResponse     = builder.add(Flow.fromFunction[EngineCapsule, EngineMessage](transfromCapToMsg))
      val responseMerge     = builder.add(Merge[EngineMessage](1))

      addUUID ~> capsuleToResponse ~> responseMerge
      // requestEnrichment ~> broadcastUUID ~> reqToResponse ~> responseMerge

      FlowShape(addUUID.in, responseMerge.out)
    }.named("core-flow")
    return Flow.fromGraph(graph);
  }

  private def enrichWithUUID(message:ClientMessage):EngineCapsule = {
    return new EngineCapsuleBase(
      Seq("addUUID"),
      Map[String, Any](),
      EngineCapsuleStatuses.Ok,
      None,
      message,
      Neo4JHelper.uuid
    )
  }

  private def transfromCapToMsg(request: EngineCapsule):EngineMessage = {
    return new EngineMessageBase(
      request.id,
      request.status.name,
      request.message.payload /*NOTE: This is just for the moment.*/
    )
  }

  /**
  An immutable message issued by a remote client.
  */
  trait ClientMessage{
    def payload: String
    def time: Long //When the request was issued
  }

  /**
  An immutable internal data element passed between flows.
  Intented to encapsulate generic data through the flow.
  */
  trait EngineCapsule{
    /**
    The sequence of stops the capsule has been passed through.
    */
    def auditTrail: Seq[String]

    /**
    Returns a deep copy of the capsule with an additional
    audit entry.
    */
    def record(stop: String):EngineCapsule

    /**
    Returns a deep copy of the capsule with a new attributed added.
    */
    def enrich(key: String, value:Any):EngineCapsule

    /**
    The associated attributes on the capsule.
    */
    def attributes:Map[String, Any]

    /**
    The current status of the capsule.
    */
    def status: EngineCapsuleStatus

    /**
    Optional associated error message. Only relevent if
    the status is EngineCapsuleStatuses.Error.
    */
    def errorMessage: Option[String]

    /**
    The associated message.
    */
    def message: ClientMessage

    /**
    The unique identifier of the capsule. Formated as a
    type IV UUID.
    */
    def id: String
  }

  /**
  An immutable message initiated by the Engine to a client.
  */
  trait EngineMessage{
    def id: String
    def status: String
    def textMessage: String
    override def toString:String = {
      s"""
      |id: $id  status: $status
      |Text Message:
      |$textMessage
      """.stripMargin
    }
  }

  /**
  Base implimentation of the ClientMessage trait.
  */
  class ClientMessageBase(requestMsg:String) extends ClientMessage{
    val creationTime = Neo4JHelper.time
    def payload:String = requestMsg
    def time:Long = creationTime
  }

  sealed trait EngineCapsuleStatus{
    def name:String
  }

  object EngineCapsuleStatuses{
      case object Ok extends EngineCapsuleStatus{ val name = "Ok"}
      case object Error extends EngineCapsuleStatus{ val name = "Error"}
  }

  /**
  Base implimentation of the EngineCapsule trait.
  */
  class EngineCapsuleBase(
    val auditTrail: Seq[String],
    val attributes: Map[String, Any],
    val status: EngineCapsuleStatus,
    val errorMessage: Option[String],
    val message: ClientMessage,
    val id: String
  ) extends EngineCapsule{
    def enrich(key: String, value:Any):EngineCapsule = {
      val newAtts = attributes.+(key -> value)
      return new EngineCapsuleBase(
        auditTrail,
        newAtts,
        status,
        errorMessage,
        message,
        id)
    }

    def record(stop: String):EngineCapsule = {
      val stops = auditTrail:+(stop)
      return new EngineCapsuleBase(
        stops,
        attributes,
        status,
        errorMessage,
        message,
        id
      )
    }
  }

  class EngineMessageBase(
    val id:String,
    val status:String,
    val textMessage:String
  ) extends EngineMessage{}
}
