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
      val sink = Sink.seq[CoreFlow.EngineMessage]
      val runnable: RunnableGraph[Future[Seq[CoreFlow.EngineMessage]]] =
        clientSource.via(CoreFlow.flow).toMat(sink)(Keep.right)

      val result: Future[Seq[CoreFlow.EngineMessage]] = runnable.run()

      result.onSuccess{
        case r => {
          r should have length(2)
          // r.foreach(i => Console.println(i))
        }
      }
    }

    def requests():Seq[CoreFlow.ClientMessageBase] = {
      Vector(new CoreFlow.ClientMessageBase("A"),
        new CoreFlow.ClientMessageBase("B"))
    }
  }
}
