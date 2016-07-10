package org.machine.engine.flow

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}
import scala.concurrent.{ExecutionContext, Future}

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import com.typesafe.config._

import org.machine.engine.Engine
import org.machine.engine.TestUtils

class WebSocketFlowSpec extends TestKit(ActorSystem("WebSocketFlowSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{
  import TestUtils._

  private val config = ConfigFactory.load()
  var engine:Engine = null
  var notesDataSetId:String = null
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val sink = Sink.seq[Message]

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    notesDataSetId = engine.createDataSet("notes", "My collection of notes.")
  }

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
    perge
  }

  describe("Websocket Flow"){
    it("should process the list of datasets associated with a user"){
      val cmd = """
      |{
      | "user": "Sam",
      | "actionType": "Retrieve",
      | "scope": "UserSpace",
      | "entityType": "DataSet",
      | "filter": "All"
      |}
      """.stripMargin.replaceAll("\t","")

      val runnable: RunnableGraph[Future[Seq[Message]]] = createClientSource(cmd).via(WebSocketFlow.flow).toMat(sink)(Keep.right)
      val result: Future[Seq[Message]] = runnable.run()

      result.onSuccess{
        case r => {
          Console.println(r) //should be a Seq of TextMessages. Should get a receipt and response
        }
      }

      result.onFailure{
        case error => {
          Console.println(error)
          fail()
        }
      }
    }
  }

  def createClientSource(msg: String) = Source.single(TextMessage(msg))
}
