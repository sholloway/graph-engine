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

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config._
import org.machine.engine.Engine

import akka.http.scaladsl.unmarshalling.Unmarshal

class HttpRequestsSpec extends TestKit(ActorSystem("AkkaHTTPSpike")) with ImplicitSender
  with FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import HttpMethods._
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
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
    describe ("HTTP Requests"){
      it ("should return static message for root"){
        val responseFuture = Http().singleRequest(HttpRequest(GET, uri = s"$scheme://$host:$port"))
        val expected = "<html><body>This is a private channel for engine communication.</body></html>"
        verifyHTTPRequest(responseFuture, expected, 1.second)
      }

      it ("should return static message for /"){
        val responseFuture = Http().singleRequest(HttpRequest(GET, uri = s"$scheme://$host:$port/"))
        val expected = "<html><body>This is a private channel for engine communication.</body></html>"
        verifyHTTPRequest(responseFuture, expected, 1.second)
      }

      it ("should provide usage stats for /stats")(pending)

      it ("should provide configuration for /configuration"){
        val responseFuture = Http().singleRequest(HttpRequest(GET, uri = s"$scheme://$host:$port/configuration"))
        val expected = s"<html><body><h1>Engine</h1><hr/>Version:$engineVersion</body></html>"
        verifyHTTPRequest(responseFuture, expected, 1.second)
      }

      it ("should provide the diagram of the in memory decision tree for /rules")(pending)
    }
  }

  def normalize(msg: String):String = {
    msg.replaceAll("\t","").replaceAll("\n","").replaceAll(" ", "")
  }

  def verifyHTTPRequest(responseFuture: Future[HttpResponse], expected: String, timeout: FiniteDuration, log: Boolean = false) = {
    whenReady(responseFuture) { response =>
      response match {
        case HttpResponse(StatusCodes.OK, headers, entity, _) => {
          val bs: Future[ByteString] = entity.toStrict(timeout).map { _.data }
          val s: Future[String] = bs.map(_.utf8String)
          whenReady(s){ payload =>
            if(log){
              println(payload)
            }
            normalize(payload) should equal(normalize(expected))
          }
        }
        case HttpResponse(code, _, _, _) =>
          println("Request failed, response code: " + code)
          fail()
      }
    }
  }
}
