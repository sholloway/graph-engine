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
  import LoginHelper._

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  private val config = ConfigFactory.load()
  val server = new WebServer()
  val dbPath = config.getString("engine.graphdb.path")
  val dbFile = new File(dbPath)
  var engine:Engine = null
  val engineVersion = config.getString("engine.version")

  val goodUserName  = "tech49"
  val badUserName = "Jack"
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
        val user:String = "bad user name"
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val response = createUser(credentials(user, pwd))
        response._1 should equal(403)
      }

      it ("should return 401 for a bad password"){
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = "bad password"
        val response = createUser(credentials(user, pwd))
        response._1 should equal(401)
      }

      it ("should allow access (200) if the username and password are correct"){
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val serviceCreds = credentials(user, pwd)
        val response = createUser(serviceCreds)
        response._1 should equal(200)
        cleanUpUser(response._2)
      }
    }

    describe("Users"){
      it ("should create a new user"){
        val user:String = config.getString("engine.communication.identity_service.user")
        val pwd:String = config.getString("engine.communication.identity_service.password")
        val response = createUser(credentials(user, pwd))
        response._1 should equal(200)

        val responseMap = strToMap(response._2)
        val userId = responseMap.get("userId").get.toString()
        val engineUser = findUserById(userId)

        engineUser.firstName should be("Jack")
        engineUser.lastName should be("Harper")
        engineUser.userName should be("tech49")
        engineUser.emailAddress should be("jharper@missioncontrol.com")
        cleanUpUser(response._2)
      }
    }

    describe("Login"){
      it ("should return 200 and session token after authenticating a user"){
        val serviceCreds = serviceCredentials()
        val newUserResponse = createUser(serviceCreds)
        val loginResponse = loginAttempt(goodUserName, goodUserPwd, serviceCreds)
        val userJack = findUserByUserName("tech49")
        val expectedMsg = s"""{"userId":"${userJack.id}"}"""
        loginResponse._1 should equal(200)
        loginResponse._2 should equal(expectedMsg)
        loginResponse._3 should not be(null)
        cleanUpUser(newUserResponse._2)
      }

      it ("should return 401 and no session token if the user's username is incorrect."){
        val serviceCreds = serviceCredentials()
        val response = createUser(serviceCreds)
        loginAttempt(badUserName, goodUserPwd, serviceCreds)._1 shouldBe 401
        cleanUpUser(response._2)
      }

      it ("should return 401 and no session token if the user's password is incorrect."){
        val serviceCreds = serviceCredentials()
        val response = createUser(serviceCreds)
        loginAttempt(goodUserName, badUserPwd, serviceCreds)._1 shouldBe 401
        cleanUpUser(response._2)
      }

      it ("should logout the user"){
        val serviceCreds = serviceCredentials()
        val response = createUser(serviceCreds)
        response._1 should equal(200)
        val jwt = login(serviceCreds)
        attemptToGetProtectedResource(200, jwt, serviceCreds)
        logout(jwt,serviceCreds)
        attemptToGetProtectedResource(401, jwt, serviceCreds)
        cleanUpUser(response._2)
      }
    }
  }

  def cleanUpUser(newUserStr: String) = {
    val responseMap = strToMap(newUserStr)
    val userId = responseMap.get("userId").get.toString()
    deleteUser(serviceCredentials(), userId)
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
}
