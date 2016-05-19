package org.machine.engine.flow

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}

import scala.concurrent.{ExecutionContext, Future}

class WebSocketFlowSpec extends TestKit(ActorSystem("WebSocketFlowSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{

  // override def beforeAll(){}

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
  }

  describe("Websocket Flow"){
    /*
    TODO:
    As soon as I can send a message through the flow, attempt to wire it up
    to the actual WS server.
    */
    ignore("should do stuff"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val clientSource = Source.single(TextMessage("Hello World")) //This is probably not valid...
      val sink = Sink.seq[Message]

      val runnable: RunnableGraph[Future[Seq[Message]]] =
        clientSource.via(WebSocketFlow.flow).toMat(sink)(Keep.right)

      val result: Future[Seq[Message]] = runnable.run()

      result.onSuccess{
        case r => {
          Console.println(r) //should be a Seq of TextMessages
        }
      }
    }
  }
}
