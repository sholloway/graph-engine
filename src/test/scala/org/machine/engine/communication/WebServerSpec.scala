package org.machine.engine.communication

import java.io.File;
import java.io.IOException;
import org.neo4j.io.fs.FileUtils

import org.scalatest._
import org.scalatest.mock._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.actor.{Actor, Props}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.model.headers.{ BasicHttpCredentials, Authorization }

import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config._
import org.machine.engine.Engine

class WebServerSpec extends TestKit(ActorSystem("AkkaHTTPSpike")) with ImplicitSender
  with FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import HttpMethods._
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  import system.dispatcher

  private val config = ConfigFactory.load()
  val server = new WebServer()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null

  val scheme = "http"
  val host = config.getString("engine.communication.webserver.host")
  val port = config.getString("engine.communication.webserver.port")
  val engineVersion = config.getString("engine.version")
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
    println("started after all")
    server.stop()
    TestKit.shutdownActorSystem(system)
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      it ("should echo commands for /ping"){
        val printSink: Sink[Message, Future[Done]] = createPrintSink()
        val helloSource: Source[Message, NotUsed] = Source.single(TextMessage("hello world!"))

        // the Future[Done] is the materialized value of Sink.foreach
        // and it is completed when the stream completes
        val flow: Flow[Message, Message, Future[Done]] = Flow.fromSinkAndSourceMat(printSink, helloSource)(Keep.left)

        // upgradeResponse is a Future[WebSocketUpgradeResponse] that
        // completes or fails when the connection succeeds or fails
        // and closed is a Future[Done] representing the stream completion from above
        val (upgradeResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest(echoPath, subprotocol = Some("engine.json.v1")), flow)

        val connected = upgradeResponse.map { upgrade =>
          // just like a regular http request we can get 404 NotFound,
          // with a response body, that will be available from upgrade.response
          println("Upgrade Response Recieved")
          if (upgrade.response.status == StatusCodes.OK) {
            println("200 Received")
            Done
          } else if(upgrade.response.status == StatusCodes.SwitchingProtocols){
            println("101 Recieved")
          }else {
            throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
          }
        }

        // in a real application you would not side effect here
        // and handle errors more carefully
        connected.onComplete(println)
        connected.onFailure{
          case e => {
            println(e)
            fail()
          }
        }
        closed.foreach(_ => println("closed"))

        whenReady(connected){ response =>
          println("stupid...")
          println(response)
        }
      }

      it ("should AssociateElements")(pending)
      it ("should CommandFactory")(pending)
      it ("should CommandScope")(pending)
      it ("should CreateDataSet")(pending)
      it ("should CreateElement")(pending)
      it ("should CreateElementDefinition")(pending)
      it ("should DeleteAssociation")(pending)
      it ("should DeleteElement")(pending)
      it ("should DeleteElementDefintion")(pending)
      it ("should EditAssociation")(pending)
      it ("should EditDataSet")(pending)
      it ("should EditElement")(pending)
      it ("should EditElementDefintion")(pending)
      it ("should EditElementPropertyDefinition")(pending)
      it ("should EngineCmdResult")(pending)
      it ("should EngineCommands")(pending)
      it ("should FindAssociationById")(pending)
      it ("should FindDataSetById")(pending)
      it ("should FindDataSetByName")(pending)
      it ("should FindDownStreamElementsByElementId")(pending)
      it ("should FindElementById")(pending)
      it ("should FindElementDefinition")(pending)
      it ("should FindElementDefinitionById")(pending)
      it ("should FindElementDefinitionByName")(pending)
      it ("should FindInboundAssociationsByElementId")(pending)
      it ("should FindOutboundAssociationsByElementId")(pending)
      it ("should FindUpStreamElementsByElementId")(pending)
      it ("should GraphCommandOptions")(pending)
      it ("should InternalEngineCommand")(pending)
      it ("should ListAllElementDefinitions")(pending)
      it ("should ListDataSets")(pending)
      it ("should RemoveAssociationField")(pending)
      it ("should RemoveElementField")(pending)
      it ("should RemoveElementPropertyDefinition")(pending)
      it ("should RemoveInboundAssociations")(pending)
      it ("should RemoveOutboundAssociations")(pending)
    }
  }

  def createPrintSink(): Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict => {
      println("Received Response from Server:")
      println(message.text)
    }
  }
}
