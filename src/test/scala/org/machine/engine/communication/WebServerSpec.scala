package org.machine.engine.communication

import java.io.File;
import java.io.IOException;
import org.neo4j.io.fs.FileUtils

import org.scalatest._
import org.scalatest.mock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import akka.actor.ActorSystem
import akka.actor.{Actor, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config._
import org.machine.engine.Engine
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class WebServerSpec extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import WSHelper._

  //Configure the whenReady for how long to wait.
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  val server = new WebServer()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null

  val scheme = "http"
  val host = config.getString("engine.communication.webserver.host")
  val port = config.getString("engine.communication.webserver.port")
  val engineVersion = config.getString("engine.version")
  val enginePath = s"ws://$host:$port/ws"
  val echoPath = s"ws://$host:$port/ws/ping"

  /*
  TODO Test the WebServer
  1. Get an instance of the engine.
  2. Create a set of data that can be used for all 36 commands.
  3. Test the execution of all 41 rules (36 commands).
  */
  override def beforeAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
    server.start()
  }

  override def afterAll(){
    server.stop()
    // TestKit.shutdownActorSystem(system)
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      it ("should echo commands for /ping"){
        val request = Source.fromIterator(() => Seq(tm("A"), tm("B"), tm("C"), tm("D"), tm("E"), tm("F")).toIterator)
        val closed = invokeWS(request, echoPath)
        whenReady(closed){ results =>
          results should equal(Vector(TextMessage.Strict("A"),
            TextMessage.Strict("B"),
            TextMessage.Strict("C"),
            TextMessage.Strict("D"),
            TextMessage.Strict("E"),
            TextMessage.Strict("F")))
        }
      }

      describe("System Space"){
        it ("should CreateElementDefinition"){
          val edSpec = Map("name"->"Mobile Device",
            "description"->"A computer that can be carried by the user.",
            "properties"->Seq(Map("name"->"Model", "propertyType"->"String", "description"->"The specific manufacture model."),
              Map("name"->"Manufacture", "propertyType"->"String", "description"->"The company that made the device.")))

          val request = buildWSRequest(user="Bob",
            actionType="Create",
            scope="SystemSpace",
            entityType="ElementDefinition",
            filter="None",
            options=edSpec)

          val closed:Future[Seq[Message]] = invokeWS(request, enginePath)

          whenReady(closed){ results =>
            results should have length 2
            val envelopeMap = msgToMap(results.last)
            envelopeMap("status") should equal("Ok")
            envelopeMap("messageType") should equal("CmdResult")
            val payloadMap = strToMap(envelopeMap("textMessage").asInstanceOf[String])
            payloadMap.contains("id") should equal(true)
            val edId = payloadMap("id").asInstanceOf[String]
            val ed = engine.inSystemSpace.findElementDefinitionById(edId)
            ed.name should equal("Mobile Device")
            ed.description should equal("A computer that can be carried by the user.")
            ed.properties should have length 2
          }
        }

        it ("should ListAllElementDefinitions")(pending)
        it ("should EditElementDefintion")(pending)
        it ("should EditElementPropertyDefinition")(pending)
        it ("should RemoveElementPropertyDefinition")(pending)
        it ("should DeleteElementDefintion")(pending)
        it ("should FindElementDefinition")(pending)
        it ("should FindElementDefinitionById")(pending)
        it ("should FindElementDefinitionByName")(pending)
      }

      describe("User Space"){
        it ("should CreateElementDefinition")(pending)
        it ("should ListAllElementDefinitions")(pending)
        it ("should EditElementDefintion")(pending)
        it ("should EditElementPropertyDefinition")(pending)
        it ("should RemoveElementPropertyDefinition")(pending)
        it ("should DeleteElementDefintion")(pending)
        it ("should FindElementDefinition")(pending)
        it ("should FindElementDefinitionById")(pending)
        it ("should FindElementDefinitionByName")(pending)

        it ("should CreateDataSet")(pending)
        it ("should ListDataSets")(pending)

        it ("should EditDataSet")(pending)
        it ("should FindDataSetById")(pending)
        it ("should FindDataSetByName")(pending)
      }

      describe("Datasets"){
        it ("should CreateElementDefinition")(pending)
        it ("should ListAllElementDefinitions")(pending)
        it ("should EditElementDefintion")(pending)
        it ("should EditElementPropertyDefinition")(pending)
        it ("should RemoveElementPropertyDefinition")(pending)
        it ("should DeleteElementDefintion")(pending)
        it ("should FindElementDefinition")(pending)
        it ("should FindElementDefinitionById")(pending)
        it ("should FindElementDefinitionByName")(pending)

        it ("should CreateElement")(pending)
        it ("should DeleteAssociation")(pending)
        it ("should DeleteElement")(pending)

        it ("should AssociateElements")(pending)
        it ("should EditAssociation")(pending)
        it ("should RemoveInboundAssociations")(pending)
        it ("should RemoveOutboundAssociations")(pending)

        it ("should EditElement")(pending)

        it ("should FindDownStreamElementsByElementId")(pending)
        it ("should FindElementById")(pending)

        it ("should FindAssociationById")(pending)
        it ("should FindInboundAssociationsByElementId")(pending)
        it ("should FindOutboundAssociationsByElementId")(pending)
        it ("should FindUpStreamElementsByElementId")(pending)
        it ("should RemoveAssociationField")(pending)
        it ("should RemoveElementField")(pending)
      }
    }
  }
}