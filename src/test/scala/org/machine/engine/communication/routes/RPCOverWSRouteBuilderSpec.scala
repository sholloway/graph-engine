package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.client._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge}
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit._
import akka.stream.{ActorMaterializer}
import akka.stream.scaladsl._

import com.typesafe.config._

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.communication.WSHelper
import org.scalatest._

class RPCOverWSRouteBuilderSpec extends FunSpecLike
  with Matchers
  with BeforeAndAfterAll
  with ScalatestRouteTest{
  import TestUtils._

  val config = ConfigFactory.load()
  val username = config.getString("engine.communication.webserver.user")
  val pwd = config.getString("engine.communication.webserver.password")
  val validCredentials = BasicHttpCredentials(username, pwd)
  val invalidCredentials = BasicHttpCredentials("bad user", "bad password")
  val wsClient = WSProbe()
  val routes = RPCOverWSRouteBuilder.buildRoutes()

  var engine:Engine = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
  }

  override def afterAll(){
    perge
  }

  it ("should reject basd credentials basic auth"){
    WS("/ws", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(invalidCredentials) ~>
      Route.seal(routes) ~> check {
      status shouldEqual StatusCodes.Unauthorized
      responseAs[String] shouldEqual "The supplied authentication is invalid"
      header("WWW-Authenticate").get.value shouldEqual("Basic realm=\"Engine User Service\",charset=UTF-8")
    }
  }

  it ("should accept valid credentials"){
    WS("/ws", wsClient.flow, Seq("engine.json.v1")) ~>
      addCredentials(validCredentials) ~>
      routes ~> check {
      isWebSocketUpgrade shouldEqual true

      val message = "Hello World"
      val requestOpts = Map("message"->message)

      val request:String = WSHelper.buildWSRequestStr(user="Bob",
        actionType="Echo",
        scope="SystemSpace",
        entityType="ElementDefinition",
        filter="None",
        options=requestOpts)

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
