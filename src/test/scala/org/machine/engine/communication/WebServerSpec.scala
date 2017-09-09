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
import org.machine.engine.TestUtils
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class WebServerSpec extends FunSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  with BeforeAndAfterEach{
  import WSHelper._
  import TestUtils._
  import LoginHelper._

  //Configure the whenReady for how long to wait.
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  val server = new WebServer()
  private val serviceCreds = serviceCredentials()
  private val PROTOCOL: String = "engine.json.v1"
  private var jwtSessionToken:String = null
  private var activeUserId:String = null
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null

  val scheme = "http"
  val host = config.getString("engine.communication.webserver.host")
  val port = config.getString("engine.communication.webserver.port")
  val engineVersion = config.getString("engine.version")
  val enginePath = s"ws://$host:$port/ws"
  val echoPath = s"ws://$host:$port/ws/ping"

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
    val newUserResponse = createUser(serviceCreds)
    activeUserId = getUserId(newUserResponse._2)
    jwtSessionToken = login(serviceCreds)
  }

  override def afterAll(){
    server.stop()
    perge
  }

  override def afterEach(){
    engine.reset()
  }

  describe("Receiving Requests"){
    describe("WebSocket Requests"){
      it ("should echo commands for /ping"){
        val request = Source.fromIterator(() => Seq(tm("A"), tm("B"), tm("C"), tm("D"), tm("E"), tm("F")).toIterator)
        // val closed = invokeWS(request, echoPath)
        val closed = invokeWS(request, echoPath, PROTOCOL, jwtSessionToken)
        whenReady(closed){ results =>
          results should equal(Vector(TextMessage.Strict("A"),
            TextMessage.Strict("B"),
            TextMessage.Strict("C"),
            TextMessage.Strict("D"),
            TextMessage.Strict("E"),
            TextMessage.Strict("F")))
        }
      }

      it ("Should gracefully handle requests that do not match to decisions")(pending)

      it ("Should gracefully handle requests missing parameters")(pending)
    }
  }
}
