package org.machine.engine.flow

import org.scalatest._
import org.scalatest.mock._

import akka.actor.ActorSystem
import akka.testkit.{ TestActors, TestKit, ImplicitSender }

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, SourceShape}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Merge, MergePreferred, RunnableGraph, Source, Sink}

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import com.typesafe.config._

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.flow.requests._

class GraphCmdWorkerFlowSpec extends TestKit(ActorSystem("GraphCmdWorkerFlowSpec"))
  with ImplicitSender
  with FunSpecLike
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach{
  import TestUtils._
  private val config = ConfigFactory.load()
  private var engine:Engine = null
  private var notesDataSetId:String = null
  private var noteElementDefininitionId:String = null
  private var activeUserId:String = null

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
    notesDataSetId = engine.forUser(activeUserId)
      .createDataSet("notes", "My collection of notes.")
  }

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
    perge
  }

  describe("Graph Command Worker Flow"){
    it("should retrieve the list of datasets"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val request = RequestMessage(activeUserId,
        "Retrieve",
        "UserSpace",
        "DataSet",
        "All")

      val requestStr = RequestMessage.toJSON(request)
      val capsule = EngineCapsule(ClientMessage(requestStr), "123")
      val clientSource = Source.single(capsule.enrich("deserializedMsg", request))
      val sink = Sink.seq[EngineMessage]

      val runnable: RunnableGraph[Future[Seq[EngineMessage]]] =
        clientSource.via(GraphCmdWorkerFlow.flow).toMat(sink)(Keep.right)

      val result: Future[Seq[EngineMessage]] = runnable.run()

      result.onSuccess{
        case r => {
          Console.println("Flow Ran Succesfully!")
          r.length should be(1)
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
}
