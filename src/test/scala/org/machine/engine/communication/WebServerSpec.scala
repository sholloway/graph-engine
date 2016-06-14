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
import org.machine.engine.exceptions._
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

      it ("Should gracefully handle requests that do not match to decisions")(pending)

      it ("Should gracefully handle requests missing parameters")(pending)
    }
  }
}
