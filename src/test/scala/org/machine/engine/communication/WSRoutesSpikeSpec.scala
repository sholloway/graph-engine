package org.machine.engine.communication

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, UpgradeToWebSocket}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.{ActorMaterializer}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.util.Timeout

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit._
import akka.http.scaladsl.server._

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock._
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import org.machine.engine.TestUtils

class WSSpikeServer{
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap.
  private implicit val executionContext = system.dispatcher
  private val config = system.settings.config
  private var bindingFutureOption:Option[Future[Http.ServerBinding]] = None;

  def start(){
    bindingFutureOption = initializeEndpoint()
  }

  def stop(){
    bindingFutureOption.foreach{ future =>
      future.flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
    }
  }

  def initializeEndpoint():Option[Future[Http.ServerBinding]] = {
    val routes = createRoutes()
    val wsHost = "localhost"
    val wsPort = 8999
    return Some(Http().bindAndHandle(routes, wsHost, wsPort))
  }

  def createRoutes():Route = {
    val routes = {
      akka.http.scaladsl.server.Directives.path("services" / "greeter") {
        handleWebSocketMessagesForProtocol(greeterService, "engine.json.v1")
      }~
      akka.http.scaladsl.server.Directives.path("services" / "echo") {
        handleWebSocketMessagesForProtocol(echoService, "engine.json.v1")
      }
    }
    return routes
  }

  def greeterService(): Flow[Message, Message, Any] = Flow[Message].mapConcat {
    case tm: TextMessage => {
      val msg = tm.asInstanceOf[TextMessage.Strict].text
      val response = s"Hello $msg!"
      TextMessage(response) :: Nil
    }
    case bm: BinaryMessage =>{
      // ignore binary messages but drain content to avoid the stream being clogged
      bm.dataStream.runWith(Sink.ignore)
      Nil
    }
  }

  def echoService(): Flow[Message, Message, Any] =
    // needed because a noop flow hasn't any buffer that would start processing in tests
    Flow[Message].buffer(1, OverflowStrategy.backpressure)
}

class WSRoutesSpikeSpec extends FunSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with ScalatestRouteTest{
  import WSHelper._
  import TestUtils._

  //Configure the whenReady for how long to wait.
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val server = new WSSpikeServer()
  val echoPath = s"ws://localhost:8999/services/echo"
  val greeterPath = s"ws://localhost:8999/services/greeter"

  override def beforeAll(){
    server.start()
  }

  override def afterAll(){
    server.stop()
  }

  describe("WebSockets with High Level DSL"){
    it ("should get echo responses from the websocket"){
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

    it ("should be greeted"){
      val request = Source.fromIterator(() => Seq(tm("A"), tm("B"), tm("C"), tm("D"), tm("E"), tm("F")).toIterator)
      val closed = invokeWS(request, greeterPath)
      whenReady(closed){ results =>
        results should equal(Vector(TextMessage.Strict("Hello A!"),
          TextMessage.Strict("Hello B!"),
          TextMessage.Strict("Hello C!"),
          TextMessage.Strict("Hello D!"),
          TextMessage.Strict("Hello E!"),
          TextMessage.Strict("Hello F!")))
      }
    }

    it ("should use test kit"){
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      val wsClient = WSProbe()
      WS("/services/greeter", wsClient.flow, Seq("engine.json.v1")) ~> server.createRoutes() ~> check {
        // check response for WS Upgrade headers
        isWebSocketUpgrade shouldEqual true

        // manually run a WS conversation
        wsClient.sendMessage("Peter")
        wsClient.expectMessage("Hello Peter!")

        // wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
        // wsClient.expectNoMessage(100.millis)

        wsClient.sendMessage("John")
        wsClient.expectMessage("Hello John!")

        wsClient.sendCompletion()
        wsClient.expectCompletion()
      }
    }
  }
}
