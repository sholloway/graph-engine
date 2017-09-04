package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.client._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge}
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.stream.{ActorMaterializer}
import akka.stream.scaladsl._
import com.typesafe.config._

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.communication.{WSHelper, LoginHelper, WebServer}
import org.scalatest._

class RPCOverWSRouteBuilderSpec extends FunSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ScalatestRouteTest{
  import TestUtils._
  import LoginHelper._

  val config = ConfigFactory.load()
  val username = config.getString("engine.communication.webserver.user")
  val pwd = config.getString("engine.communication.webserver.password")
  val SESSION_HEADER:String = config.getString("engine.communication.webserver.session.header")
  val validCredentials = BasicHttpCredentials(username, pwd)
  val invalidCredentials = BasicHttpCredentials("bad user", "bad password")
  val wsClient = WSProbe()
  val routes = RPCOverWSRouteBuilder.buildRoutes()

  var engine:Engine = null
  val server = new WebServer()
  var jwtToken:String = null
  val serviceCreds = serviceCredentials()

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
    createUser(serviceCreds)
    jwtToken = login(serviceCreds)
  }

  override def afterAll(){
    server.stop()
    perge
  }

  it ("should reject bad credentials basic auth"){
    WS("/ws", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(invalidCredentials) ~>
        Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.Unauthorized
          responseAs[String] shouldEqual "The supplied authentication is invalid"
          header("WWW-Authenticate").get.value shouldEqual("Basic realm=\"Engine User Service\",charset=UTF-8")
        }
  }

  it ("should return a 400 if the request does not contain a value for session header"){
    WS("/ws/ping", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(validCredentials) ~>
        Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.BadRequest
        }
  }

  it ("should return a 401 is the session is not valid"){
    WS("/ws/ping", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(validCredentials) ~> addHeader(SESSION_HEADER, "fake session") ~>
        Route.seal(routes) ~> check {
          status shouldEqual StatusCodes.Unauthorized
        }
  }

  it ("should attempt WebSocket upgrade when a valid session is present" ){
    WS("/ws/ping", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(validCredentials) ~> addHeader(SESSION_HEADER, jwtToken) ~>
        Route.seal(routes) ~> check {
          isWebSocketUpgrade shouldEqual true
          val message = "Hello World"
          wsClient.sendMessage(message)
          wsClient.expectMessage(message)
          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
  }

  it ("should accept valid credentials"){
    WS("/ws", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(validCredentials) ~> routes ~> check {
        isWebSocketUpgrade shouldEqual true
        val message = "Hello World"
        val requestOpts = Map("message"->message)
        val request:String = WSHelper.buildWSRequestStr("Bob",
          "Echo","SystemSpace", "ElementDefinition", "None",requestOpts)
          wsClient.sendMessage(request)
          // Don't currently have the ability to inspect parts
          // of the message. So not asserting on it since the
          // ID is always unique.
          // wsClient.expectMessage(TextMessage("{"id":"f2b7099e-1c64-4114-8181-099abfdce7fd","status":"Ok","messageType":"CmdResult","textMessage":"{\n  \"status\":\"OK\",\n  \"id\":\"Hello World\"\n}"}"))

          // wsClient.sendCompletion()
          // wsClient.expectCompletion()
      }
  }
}
