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

class WebSocketFlowSpec extends TestKit(ActorSystem("WebSocketFlowSpec"))
  with ImplicitSender
  with FunSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach{
  import TestUtils._

  private val config = ConfigFactory.load()
  private var engine:Engine = null
  private var activeUserId:String = null
  private var notesDataSetId:String = null
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private val sink = Sink.seq[Message]

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    activeUserId = Engine.getInstance
      .createUser
      .withFirstName("Bob")
      .withLastName("Grey")
      .withEmailAddress("onebadclown@derry-maine.com")
      .withUserName("pennywise")
      .withUserPassword("You'll float too...")
    .end
    notesDataSetId = engine.forUser(activeUserId).createDataSet("notes", "My collection of notes.")
  }

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
    perge
  }

  describe("Websocket Flow"){
    it("should process the list of datasets associated with a user"){
      val cmd = s"""
      |{
      | "userId": "${activeUserId}",
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
          // println(r) //should be a Seq of TextMessages. Should get a receipt and response
          //Should return a receipt and response
          r.length should be(2)
        }
      }

      result.onFailure{
        case error => {
          println(s"An error occured: ${error}")
          fail()
        }
      }
    }
  }

  def createClientSource(msg: String) = Source.single(TextMessage(msg))
}
