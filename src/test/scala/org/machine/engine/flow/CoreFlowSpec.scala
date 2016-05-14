package org.machine.engine.flow

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
// import akka.actor.{Actor, Props}

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}

import org.machine.engine.graph.Neo4JHelper

class CoreFlowSpec extends TestKit(ActorSystem("CoreFlowSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  // override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  describe("Core Engine Flow"){
    it("should respond to all requests with a UUID"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val clientSource = Source(requests().toList)
      val sink = Sink.seq[EngineMessage]
      val runnable: RunnableGraph[Future[Seq[EngineMessage]]] =
        clientSource.via(CoreFlow.flow).toMat(sink)(Keep.right)

      val result: Future[Seq[EngineMessage]] = runnable.run()

      result.onSuccess{
        case r => {
          val results        = r.groupBy(_.status)
          val receiptsCount  = results("Ok").length
          val processedCount = results("Processed").length
          val errorCount     = results("Error").length
          receiptsCount should equal(processedCount+errorCount)
        }
      }
    }

    def requests():Seq[ClientMessageBase] = {
      Vector(new ClientMessageBase("A"),
        new ClientMessageBase("B"),
        new ClientMessageBase("C"),
        new ClientMessageBase("D"),
        new ClientMessageBase("E"),
        new ClientMessageBase("F"),
        new ClientMessageBase("G"),
        new ClientMessageBase("H"),
        new ClientMessageBase("I"),
        new ClientMessageBase("J"),
        new ClientMessageBase("K")
      )
    }
  }
}
