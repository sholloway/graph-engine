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
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

import net.liftweb.json._
import net.liftweb.json.DefaultFormats

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
  val echoPath = s"ws://$host:$port/ws"
  val enginePath = s"ws://$host:$port/ws/ping"

  val failTest:PartialFunction[Throwable, Any] = {
    case e => {
      println(e)
      fail()
    }
  }

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
    TestKit.shutdownActorSystem(system)
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      ignore ("should echo commands for /ping"){
        val helloSource = Source.fromIterator(() => Seq(tm("A"), tm("B"), tm("C"), tm("D"), tm("E"), tm("F")).toIterator)
        val flow = Flow.fromSinkAndSourceMat(Sink.seq[Message], helloSource)(Keep.left)
        val (upgradeResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest(echoPath, subprotocol = Some("engine.json.v1")), flow)
        val connected = upgradeResponse.map(verifyProtocolsSwitched)
        connected.onFailure(failTest)
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
        /*
        Next Steps
        1. Refactor this test.
        2. Remove the ignore on the above test.
        3. Run all tests.
        4. Merge with Master
        5. Push to Github.com

        Notes
        Could I use a Test Fixture for some of the setup?
        */
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

          val closed:Future[Seq[Message]] = invokeWS(request)

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

  def buildWSRequest(user: String,
    actionType: String,
    scope: String,
    entityType: String,
    filter: String, options: Map[String, Any]):Source[Message, NotUsed] = {
    val rm = RequestMessage(user, actionType, scope, entityType, filter, options)
    val json = RequestMessage.toJSON(rm)
    return Source.single(TextMessage(json))
  }

  /*
  Invoke the websocket and return a future with the Sink
  captured as a sequence of responses.
  */
  def invokeWS(request: Source[Message, NotUsed],
    path: String = echoPath,
    protocol: String = "engine.json.v1"):Future[Seq[Message]] = {
    val flow = Flow.fromSinkAndSourceMat(Sink.seq[Message], request)(Keep.left)
    val (upgradeResponse, closed) = Http().singleWebSocketRequest(WebSocketRequest(path, subprotocol = Some(protocol)), flow)
    val connected = upgradeResponse.map(verifyProtocolsSwitched)
    connected.onFailure(failTest)
    return closed
  }

  def msgToMap(msg: Message):Map[String, Any] = {
    val txtMessage = msg.asInstanceOf[TextMessage.Strict]
    val envelopeDom = parse(txtMessage.text)
    return envelopeDom.values.asInstanceOf[Map[String, Any]]
  }

  def strToMap(str: String):Map[String, Any] = {
    val payloadDom = parse(str)
    return payloadDom.values.asInstanceOf[Map[String, Any]]
  }

  def printJson(request: RequestMessage) = {
    import net.liftweb.json.JsonAST._
    import net.liftweb.json.Extraction._
    import net.liftweb.json.Printer._
    implicit val formats = net.liftweb.json.DefaultFormats
    println(prettyRender(decompose(request)))
  }

  def tm(msg: String):Message = TextMessage(msg)

  def createPrintSink(): Sink[Message, Future[Done]] = Sink.foreach {
    case message: TextMessage.Strict => {
      println("Received Response from Server:")
      println(message.text)
    }
  }

  def verifyProtocolsSwitched(upgrade: WebSocketUpgradeResponse): Done = {
    if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
      Done
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

}
