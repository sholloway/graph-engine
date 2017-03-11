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
import okhttp3.{Credentials, OkHttpClient, Request, RequestBody, Response, MediaType}
import org.neo4j.io.fs.FileUtils
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock._
import org.scalatest.time.{Millis, Seconds, Span}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import org.machine.engine.Engine
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.TestUtils
import org.machine.engine.exceptions._
import org.machine.engine.graph.commands.GraphCommandOptions
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class IdentityServiceSpec extends FunSpecLike with Matchers with ScalaFutures with BeforeAndAfterAll{
  import WSHelper._
  import TestUtils._
  import Neo4JHelper._

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
    // perge
  }

  describe("Identity Service"){
    describe("Basic Auth"){
      it ("should return 403 for a bad username if the password was ok."){
        val url = s"http://$host:$port/users"
        val user:String = "bad user name"
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val credential:String = Credentials.basic(user, pwd);
        val request = new Request.Builder()
          .url(url)
          .header("Authorization", credential)
          .build()
        val response = client.newCall(request).execute()
        response.code() should equal(403)
        response.close()
      }

      it ("should return 401 for a bad password"){
        val url = s"http://$host:$port/users"
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = "bad password"
        val credential:String = Credentials.basic(user, pwd);
        val request = new Request.Builder()
          .url(url)
          .header("Authorization", credential)
          .build()
        val response = client.newCall(request).execute()
        response.code() should equal(401)
        response.close()
      }

      it ("should allow access (200) if the username and password are correct"){
        val url = s"http://$host:$port/users"
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val credential:String = Credentials.basic(user, pwd);
        val request = new Request.Builder()
          .url(url)
          .header("Authorization", credential)
          .build()
        val response = client.newCall(request).execute()
        response.code() should equal(200)
        response.body().string() should equal("Hello World\n")
      }
    }

    describe("Users"){
      it ("should create a new user"){
        val url:String = s"http://$host:$port/users"
        val expected:String = "{\"userId\":\"123\"}"
        val requestStr:String = newUserRequest()
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val credential:String = Credentials.basic(user, pwd);
        val requestBody = RequestBody.create(jsonMediaType, requestStr)
        val request = new Request.Builder()
          .url(url)
          .post(requestBody)
          .header("Authorization", credential)
          .build()
        val response = client.newCall(request).execute()
        val resultStr = response.body().string()
        Console.println(resultStr)
        response.code() should equal(200)


        import org.machine.engine.viz.GraphVizHelper
        GraphVizHelper.visualize(engine.database)

        val responseMap = strToMap(resultStr)

        //Query the engine to find the actual vertex and compare the vertex to
        //the service response. The query needs to include system space.
        val findUser = """
        |match (s:internal_system_space)-[:registered]->(u:user)
        |where u.mid={mid}
        |return u.mid as mid,
        | u.first_name as first_name,
        | u.last_name as last_name,
        | u.email_address as email_address,
        | u.user_name as user_name,
        | u.creation_time as creation_time,
        | u.last_modified_time as last_modified_time
        """.stripMargin

        Console.println(responseMap)
        val userId = responseMap.get("userId").get.toString()
        Console.println("User ID:" + userId)
        val params = GraphCommandOptions().addOption("mid", userId)
        val usersCollection = query[User](engine.database, findUser, params.toJavaMap, userQueryMapper)
        usersCollection.length shouldBe 1
        val engineUser = usersCollection.head
        engineUser.firstName should be("Jack")
        engineUser.lastName should be("Harper")
        engineUser.userName should be("tech49")
        engineUser.emailAddress should be("jharper@missioncontrol.com")
      }
    }
  }

  def userQueryMapper(results: ArrayBuffer[User],
    record: java.util.Map[java.lang.String, Object]):Unit = {
      Console.println("Got a record!")
    val id:String = record.get("mid").toString()
    val firstName:String = record.get("first_name").toString()
    val lastName:String = record.get("last_name").toString()
    val emailAddress:String = record.get("email_address").toString()
    val userName:String = record.get("user_name").toString()
    val creationTime:String = record.get("creation_time").toString()
    results += new User(id, firstName, lastName, userName, emailAddress, creationTime, null)
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
