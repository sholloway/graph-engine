package org.machine.engine.flow

import akka.NotUsed
import akka.actor.ActorSystem
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, MergePreferred, Partition, RunnableGraph, Source, Sink}

import org.machine.engine.graph.Neo4JHelper

object CoreFlow{
  def flow:Flow[ClientMessage, EngineMessage, NotUsed] = {
    val graph = GraphDSL.create(){ implicit b: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      import scala.util.Random
      implicit val rng = new Random(123)
      val addUUID              = b.add(Flow.fromFunction[ClientMessage, EngineCapsule](enrichWithUUID))
      val broadcastUUID        = b.add(Broadcast[EngineCapsule](2))
      val deserialize          = b.add(Flow.fromFunction[EngineCapsule, EngineCapsule](deserializeRequest))
      // val logOks               = b.add(Flow[EngineCapsule].map[EngineCapsule](c => {println(c); c}))
      // val logErrors            = b.add(Flow[EngineCapsule].map[EngineCapsule](c => {println(c); c}))
      val validateClientMsgMap = b.add(Flow.fromFunction[EngineCapsule, EngineCapsule](validateClientMsg))
      val partitionByStatus    = b.add(Partition[EngineCapsule](2, capsule => partByStatus(capsule)))
      val errAndReceiptMerge   = b.add(Merge[EngineCapsule](2))
      val capsuleToResponse    = b.add(Flow.fromFunction[EngineCapsule, EngineMessage](transfromCapToMsg))

      val executeCmd           = b.add(Flow.fromFunction[EngineCapsule, EngineMessage](executeCmdPoolFlow))

      val engineMsgMerge       = b.add(Merge[EngineMessage](2))
      // val deadend              = b.add(Sink.ignore)

      /*
      Things that occure in parallel need to have the async function called on them.
      The things after broadcast for example...
      http://doc.akka.io/docs/akka/2.4.4/scala/stream/stream-parallelism.html
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

  private def transfromCapToMsg(capsule: EngineCapsule):EngineMessage = {
    return new EngineMessageBase(
      capsule.id,
      capsule.status.name,
      capsule.message.payload /*NOTE: This is just for the moment.*/
    )
  }

  /*Note:
  This will be responsible for deserialzing the client's message.
  That means conversion based JSON or Protobuf.
  For right now, just pass the original text message.
  */
  private def deserializeRequest(capsule: EngineCapsule):EngineCapsule = {
    val deserializedMsg = capsule.message.payload
    return capsule.enrich("deserializedMsg", deserializedMsg, Some("deserializeRequest"))
  }

  /*
  For now just randomly fail a message.
  Expects implicit rng = new Random(...)
  */
  private def validateClientMsg(capsule: EngineCapsule)(implicit rng:Random):EngineCapsule = {
    val newStatus = if (rng.nextBoolean) EngineCapsuleStatuses.Ok else EngineCapsuleStatuses.Error
    return capsule.setStatus(newStatus)
  }

  private def partByStatus(capsule: EngineCapsule):Int = {
    return capsule.status match {
      case EngineCapsuleStatuses.Ok => 0
      case EngineCapsuleStatuses.Error => 1
      case _ => 1
    }
  }

  private def executeCmdPoolFlow(capsule: EngineCapsule):EngineMessage ={
    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Processed.name,
      capsule.message.payload /*NOTE: This is just for the moment.*/
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
    def enrich(key: String, value:Any, audit: Option[String] = None):EngineCapsule

    /**
    The associated attributes on the capsule.
    */
    def attributes:Map[String, Any]

    /**
    The current status of the capsule.
    */
    def status: EngineCapsuleStatus

    /**
    Return a deep copy of the capsule with the status set to the new value.
    */
    def setStatus(newStatus: EngineCapsuleStatus):EngineCapsule

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

    override def toString:String = {
      var msg: String = null.asInstanceOf[String]
      if (status == EngineCapsuleStatuses.Error){
        msg = s"""
        |ID: $id Status: $status
        |Error Message
        |${errorMessage.getOrElse("")}
        """.stripMargin
      }else{
        var itr = 0
        msg = s"""
        |ID: $id Status: $status
        |Audit Trail:
        |${auditTrail.map(i => {itr+=1; s"$itr: "+i;}).mkString(" ")}
        |Attributes:
        |${attributes.keys.mkString(" ")}
        """.stripMargin
      }
      return msg
    }
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
      case object Processed extends EngineCapsuleStatus{ val name = "Processed"}
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
    def enrich(key: String, value:Any, audit: Option[String] = None):EngineCapsule = {
      val newAtts = attributes.+(key -> value)
      val mutableAudit = auditTrail.toBuffer
      audit.foreach(stop => mutableAudit += stop)
      return new EngineCapsuleBase(
        mutableAudit.toSeq,
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

    def setStatus(newStatus: EngineCapsuleStatus):EngineCapsule = {
      return new EngineCapsuleBase(
        auditTrail,
        attributes,
        newStatus,
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
