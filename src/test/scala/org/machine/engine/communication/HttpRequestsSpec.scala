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


import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config._
import org.machine.engine.Engine
import org.machine.engine.TestUtils

import akka.http.scaladsl.unmarshalling.Unmarshal

class HttpRequestsSpec extends FunSpecLike
  with Matchers with ScalaFutures with BeforeAndAfterAll{
  import WSHelper._
  import TestUtils._
  // implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  private val config           = ConfigFactory.load()
  val server                   = new WebServer()
  var engine:Engine            = null

  val scheme        = "http"
  val host          = config.getString("engine.communication.webserver.host")
  val port          = config.getString("engine.communication.webserver.port")
  val engineVersion = config.getString("engine.version")

  /*
  TODO Test the WebServer
  1. Get an instance of the engine.
  2. Create a set of data that can be used for all 36 commands.
  3. Test the execution of all 41 rules (36 commands).
  */
  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
  }

  override def afterAll(){
    server.stop()
    perge
  }

  describe("Receiving Requests"){
    describe ("HTTP Requests"){
      it ("should return static message for root"){
        val responseFuture = getHTTP(s"$scheme://$host:$port")
        val expected = "<html><body>This is a private channel for engine communication.</body></html>"
        verifyHTTPRequest(responseFuture, expected, Span(5, Seconds))
      }

      it ("should return static message for /"){
        val responseFuture = getHTTP(s"$scheme://$host:$port/")
        val expected = "<html><body>This is a private channel for engine communication.</body></html>"
        verifyHTTPRequest(responseFuture, expected, Span(5, Seconds))
      }

      it ("should provide usage stats for /stats")(pending)

      it ("should provide configuration for /configuration"){
        val responseFuture = getHTTP(s"$scheme://$host:$port/configuration")
        val expected = s"<html><body><h1>Engine</h1><hr/>Version:$engineVersion</body></html>"
        verifyHTTPRequest(responseFuture, expected, Span(5, Seconds))
      }

      it ("should provide the diagram of the in memory decision tree for /rules")(pending)
    }
  }
}
