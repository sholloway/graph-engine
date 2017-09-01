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
import org.machine.engine.TestUtils
import org.machine.engine.authentication.PasswordTools
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.exceptions._
import org.machine.engine.graph.commands.GraphCommandOptions
import org.machine.engine.graph.nodes._
import org.machine.engine.flow.requests._

class IdentityServiceSpec extends FunSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll{
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

  val goodUserPwd = "I love Sally."
  val badUserPwd = "We are not an effective team."

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    server.start()
  }

  override def afterAll(){
    server.stop()
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
        response.code() should equal(200)

        val responseMap = strToMap(resultStr)
        val userId = responseMap.get("userId").get.toString()
        val engineUser = findUserById(userId)
        engineUser.firstName should be("Jack")
        engineUser.lastName should be("Harper")
        engineUser.userName should be("tech49")
        engineUser.emailAddress should be("jharper@missioncontrol.com")
      }
    }

    describe("Login"){
      it ("should return 200 and session token after authenticating a user"){
        val url = s"http://$host:$port/login"
        val credential:String = serviceCredentials()
        val requestBody = createGoodLogin()
        val request = new Request.Builder()
          .url(url)
          .header("Authorization", credential)
          .post(requestBody)
          .build()
        val response = client.newCall(request).execute()
        response.code() should equal(200)
        val userJack = findUserByUserName("tech49")
        val expectedMsg = s"""{"userId":"${userJack.id}"}"""
        response.body().string() should equal(expectedMsg)

        val jwt = response.header("Set-Authorization")
        jwt should not be(null)
      }

      it ("should return 401 and no session token if the user's password is incorrect.")(pending)
      it ("should return 401 and no session token if the user's username is incorrect.")(pending)

      /*
      1. Login
      2. Access protected resource.
      3. Logout.
      4. Attempt to access protected resource.
      */
      it ("should logout the user"){
        val credential:String = serviceCredentials()
        val jwt = login(credential)

        import org.machine.engine.viz.GraphVizHelper
        GraphVizHelper.visualize(Engine.getInstance.database,
          s"${GraphVizHelper.wd}/viz",
          "temp.dot")
          
        attemptToGetProtectedResource(200, jwt, credential)
        logout(jwt,credential)
        attemptToGetProtectedResource(10, jwt, credential)
      }
    }
  }

  def login(credential:String):String = {
    val loginUrl = s"http://$host:$port/login"
    val requestBody:RequestBody = createGoodLogin()
    val loginRequest = new Request.Builder()
      .url(loginUrl)
      .header("Authorization", credential)
      .post(requestBody)
      .build()
    val loginResponse = client.newCall(loginRequest).execute()
    loginResponse.code() should equal(200)
    return loginResponse.header("Set-Authorization")
  }

  def logout(jwt:String, credential:String):Unit = {
    val logoutUrl = s"http://$host:$port/logout"
    val logoutRequest = new Request.Builder()
      .url(logoutUrl)
      .header("Authorization", credential)
      .header("Session", jwt)
      .build()
    val logoutResponse = client.newCall(logoutRequest).execute()
    logoutResponse.code() should equal(200)
  }

  def attemptToGetProtectedResource(expectResponse:Int,
    jwt: String,
    credential:String
  ):Unit = {
    val url = s"http://$host:$port/protected"
    val logoutRequest = new Request.Builder()
      .url(url)
      .header("Authorization", credential)
      .header("Session", jwt)
      .build()
      val logoutResponse = client.newCall(logoutRequest).execute()
      logoutResponse.code() should equal(expectResponse)
  }

  def createGoodLogin():RequestBody = {
    val encodedUserPwd = PasswordTools.strToBase64(goodUserPwd)
    val requestStr: String = s"""
    |{
    |   "userName": "tech49",
    |   "password": "${encodedUserPwd}"
    |}
    """.stripMargin
    return RequestBody.create(jsonMediaType, requestStr)
  }

  def serviceCredentials():String = {
    val user:String = config.getString("engine.communication.identity_service.user")
    val pwd:String = config.getString("engine.communication.identity_service.password")
    return Credentials.basic(user, pwd);
  }

  def findUserById(userId: String):User = {
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

    val params = GraphCommandOptions().addOption("mid", userId)
    val usersCollection = query[User](engine.database,
      findUser,
      params.toJavaMap,
      userQueryMapper)
    usersCollection.length shouldBe 1
    return usersCollection.head
  }

  def findUserByUserName(userName: String):User = {
    val findUser = """
    |match (s:internal_system_space)-[:registered]->(u:user)
    |where u.user_name={userName}
    |return u.mid as mid,
    | u.first_name as first_name,
    | u.last_name as last_name,
    | u.email_address as email_address,
    | u.user_name as user_name,
    | u.creation_time as creation_time,
    | u.last_modified_time as last_modified_time
    """.stripMargin

    val params = GraphCommandOptions().addOption("userName", userName)
    val usersCollection = query[User](engine.database,
      findUser,
      params.toJavaMap,
      userQueryMapper)
    usersCollection.length shouldBe 1
    return usersCollection.head
  }

  def userQueryMapper(results: ArrayBuffer[User],
    record: java.util.Map[java.lang.String, Object]):Unit = {
    val id:String = record.get("mid").toString()
    val firstName:String = record.get("first_name").toString()
    val lastName:String = record.get("last_name").toString()
    val emailAddress:String = record.get("email_address").toString()
    val userName:String = record.get("user_name").toString()
    val creationTime:String = record.get("creation_time").toString()
    results += new User(id, firstName, lastName, userName, emailAddress, creationTime, null)
  }

  def newUserRequest():String = {
    val encodedPwd = PasswordTools.strToBase64(goodUserPwd)
    val str = s"""
    {
      "emailAddress": "jharper@missioncontrol.com",
      "firstName": "Jack",
      "lastName": "Harper",
      "userName": "tech49",
      "password": "${encodedPwd}"
    }
    """.stripMargin
    return str
  }
}
