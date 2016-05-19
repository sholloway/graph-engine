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
import org.machine.engine.flow.requests._

class GraphCmdWorkerFlowSpec extends TestKit(ActorSystem("GraphCmdWorkerFlowSpec")) with ImplicitSender
  with FunSpecLike with Matchers with BeforeAndAfterAll{
  private val config = ConfigFactory.load()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null
  var notesDataSetId:String = null
  var noteElementDefininitionId:String = null


  override def beforeAll(){
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
    notesDataSetId = engine.createDataSet("notes", "My collection of notes.")
  }

  override def afterAll(){
    TestKit.shutdownActorSystem(system)
    engine.shutdown()
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Graph Command Worker Flow"){
    it("should retrieve the list of datasets"){
      implicit val materializer = ActorMaterializer()
      implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

      val cmd = """
      |{
      | "user": "sam",
      | "actionType": "RETRIEVE",
      | "scope": "USER_SPACE",
      | "entityType": "DATA_SET",
      | "action": "ListDataSets"
      |}
      """.stripMargin.replaceAll("\t","")

      //Change this to generate a RequestMessage from the ClientMessage.
      //Should the RequestMessage be an attribute or elevated up?
      val capsule = EngineCapsule(ClientMessage(cmd), "123")
      val requestMsg = RequestMessage.fromJSON(capsule.message.payload)
      val clientSource = Source.single(capsule.enrich("RequestMessage", requestMsg))
      val sink = Sink.seq[EngineMessage]

      val runnable: RunnableGraph[Future[Seq[EngineMessage]]] =
        clientSource.via(GraphCmdWorkerFlow.flow).toMat(sink)(Keep.right)

      val result: Future[Seq[EngineMessage]] = runnable.run()

      result.onSuccess{
        case r => {
          Console.println("Flow Ran Succesfully!")
          Console.println(r)
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