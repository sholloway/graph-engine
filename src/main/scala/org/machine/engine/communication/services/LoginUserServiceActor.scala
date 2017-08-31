package org.machine.engine.communication.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal

import com.typesafe.scalalogging.{LazyLogging}
import scala.collection.mutable.ArrayBuffer
import spray.json._

import org.machine.engine.Engine
import org.machine.engine.authentication.PasswordTools
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.nodes.Credential
import org.machine.engine.graph.commands.GraphCommandOptions

trait LoginUserServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
  implicit val loginResponseFormat = jsonFormat2(LoginResponse)
  implicit val userLoginResponseFormat = jsonFormat1(UserLoginResponse)
}

case class LoginRequest(userName: String, password: String)
case class LoginResponse(status: Int, userId: String)
case class UserLoginResponse(userId: String)

object LoginUserServiceActor {
  def props(): Props = {
    Props(classOf[LoginUserServiceActor])
  }
}

class LoginUserServiceActor extends Actor with ActorLogging{
  import Neo4JHelper._
  import PasswordTools._;
  def receive = {
    case request: LoginRequest => {
      sender() ! attemptLoginUser(request)
    }
  }

  private def attemptLoginUser(request: LoginRequest):LoginResponse = {
    val credential = findPassword(request)
    var response:LoginResponse = null;
    credential.foreach(cred => {
      val saltHash = base64ToHash(cred.passwordSalt)
      val providedPwdHash:Array[Byte] = generateHash(request.password, saltHash, cred.hashIterationCount)
      val originalPwdHash = base64ToHash(cred.passwordHash)
      val passwordMatch = compare(originalPwdHash, providedPwdHash)
      response = if (passwordMatch) LoginResponse(200, cred.userId) else LoginResponse(401, null);
    })
    return response
  }

  private def findPassword(request: LoginRequest): Option[Credential] = {
    val findPasswordQuery = """
    | match (u:user)-[:authenticates_with]->(c:credential)
    | where u.user_name = {userName}
    | return u.mid as userId,
    |   c.mid as mid,
    |   c.password_hash as passwordHash,
    |   c.password_salt as passwordSalt,
    |   c.hash_iteration_count as hashIterationCount,
    |   c.creation_time as creationTime,
    |   c.last_modified_time as lastModifiedTime
    """.stripMargin
    val params = GraphCommandOptions().addOption("userName", request.userName)
    val credentials = query[Credential](Engine.getInstance.database,
      findPasswordQuery,
      params.toJavaMap,
      credentialsMapper)
    return Some(credentials.head)
  }

  private def credentialsMapper(results: ArrayBuffer[Credential],
    record: java.util.Map[java.lang.String, Object]):Unit = {
      val userId: String = record.get("userId").toString()
      val id:String = record.get("mid").toString()
      val passwordHash:String = record.get("passwordHash").toString()
      val passwordSalt:String = record.get("passwordSalt").toString()
      val hashIterationCount:Int = record.get("hashIterationCount").asInstanceOf[Int]
      val creationTime:Long = record.get("creationTime").asInstanceOf[Long]
      val lastModifiedTime:Long = record.get("lastModifiedTime").asInstanceOf[Long]
      results += new Credential(userId, id, passwordHash, passwordSalt, hashIterationCount, creationTime, lastModifiedTime)
    }
}
