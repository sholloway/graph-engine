package org.machine.engine.communication

import akka.actor.{Actor, Props}
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.config._
import java.io.File;
import java.io.IOException;
import okhttp3.{OkHttpClient, Request, RequestBody, Response, MediaType}
import org.neo4j.io.fs.FileUtils
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock._
import org.scalatest.time.{Millis, Seconds, Span}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class IdentityServiceSpec extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import WSHelper._
  import TestUtils._

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  val server = new WebServer()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null
  val client = new OkHttpClient()
  val jsonMediaType:okhttp3.MediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");

  val scheme = "http"
  val host = config.getString("engine.communication.identity_service.host")
  val port = config.getString("engine.communication.identity_service.port")
  val engineVersion = config.getString("engine.version")

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
  }

  override def afterAll(){
    server.stop()
    perge
  }

  describe("Identity Service"){
    describe("Users"){
      it ("should perform a GET example"){
        val url = s"http://$host:$port/users"
        val request = new Request.Builder()
          .url(url)
          .build()
        val response = client.newCall(request).execute()
        response.code() should equal(200)
        response.body().string() should equal("Hello World\n")
      }

      it ("should create a new user"){
        val url = s"http://$host:$port/users"
        val expected = "{\"userId\":\"tech49\"}"
        val requestStr = newUserRequest()
        val requestBody = RequestBody.create(jsonMediaType, requestStr)
        val request = new Request.Builder()
          .url(url)
          .post(requestBody)
          .build()
        val response = client.newCall(request).execute()
        val result = response.body().string()
        Console.println(result)
        response.code() should equal(200)
        result should equal(expected)
      }
    }
  }

  def newUserRequest():String = {
    val str = """
    {
      "emailAddress": "jharper@missioncontrol.com",
      "firstName": "Jack",
      "lastName": "Harper",
      "userName": "tech49"
    }
    """.stripMargin
    return str
  }
}
