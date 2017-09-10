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
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.GraphCommandOptions

trait SessionServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val saveSessionRequestFormat = jsonFormat3(SaveUserSessionRequest)
  implicit val saveSessionResponseFormat = jsonFormat0(SaveUserSessionResponse)
  implicit val logOutUserSessionRequestFormat = jsonFormat1(LogOutUserSessionRequest)
  implicit val logOutUserSessionResponseFormat = jsonFormat0(LogOutUserSessionResponse)
  implicit val isUserSessionValidRequestFormat = jsonFormat3(IsUserSessionValidRequest)
  implicit val userSessionIsVaildFormat = jsonFormat0(UserSessionIsVaild)
  implicit val userSessionIsNotVaildFormat = jsonFormat0(UserSessionIsNotVaild)
}

case class SaveUserSessionRequest(userId: String,
  sessionId: String,
  issuedTime: Long)
case class SaveUserSessionResponse()
case class LogOutUserSessionRequest(userId:String)
case class LogOutUserSessionResponse()

case class IsUserSessionValidRequest(userId: String,
  sessionId: String,
  issuedTime: Long)
sealed trait IsSessionValidResponse{}
final case class UserSessionIsVaild() extends IsSessionValidResponse
final case class UserSessionIsNotVaild() extends IsSessionValidResponse
final case class Session(sessionId: String, issuedTime: Long)

object SessionServiceActor {
  def props(): Props = {
    Props(classOf[SessionServiceActor])
  }
}

class SessionServiceActor extends Actor with ActorLogging{
  import Neo4JHelper._

  private val saveUserSessionStmt: String = """
  |match(u:user)-[:authenticates_with]->(c:credential) where u.mid = {userId}
  |create (c)-[:is_logged_in_with]->(s:session {
  | session:{sessionId},
  | creation_time: {issuedTime}
  |})
  """.stripMargin

  private val deleteAllUserSessionsStmt: String = """
  |match(u:user)-[:authenticates_with]->(c:credential)-[:is_logged_in_with]->(s:session)
  |  where u.mid = {userId}
  |detach delete s
  """.stripMargin

  private val findUserSessionStmt: String = """
  |match(u:user)-[:authenticates_with]->(c:credential)-[:is_logged_in_with]->(s:session)
  |  where u.mid = {userId} and s.session = {sessionId} and s.creation_time = {issuedTime}
  |return s.session as sessionId,
  |  s.creation_time as issuedTime
  """.stripMargin

  def receive = {
    case request: SaveUserSessionRequest => sender() ! saveUserSession(request)
    case request: LogOutUserSessionRequest => sender() ! deleteAllUserSessions(request)
    case request: IsUserSessionValidRequest => sender() ! validateUserSession(request)
  }

  private def saveUserSession(request: SaveUserSessionRequest):SaveUserSessionResponse = {
    val params = GraphCommandOptions()
      .addOption("userId", request.userId)
      .addOption("sessionId", request.sessionId)
      .addOption("issuedTime", request.issuedTime)
    run(Engine.getInstance.database,
      saveUserSessionStmt,
      params.toJavaMap,
      emptyResultProcessor[SaveUserSessionRequest])
    return new SaveUserSessionResponse()
  }

  private def deleteAllUserSessions(request: LogOutUserSessionRequest):LogOutUserSessionResponse = {
    val params = GraphCommandOptions().addOption("userId", request.userId)
    run(Engine.getInstance.database,
      deleteAllUserSessionsStmt,
      params.toJavaMap,
      emptyResultProcessor[LogOutUserSessionRequest])
    return new LogOutUserSessionResponse()
  }

  private def validateUserSession(request: IsUserSessionValidRequest):IsSessionValidResponse = {
    val params = GraphCommandOptions()
      .addOption("userId", request.userId)
      .addOption("sessionId", request.sessionId)
      .addOption("issuedTime", request.issuedTime)
    val records = query[Session](Engine.getInstance.database,
      findUserSessionStmt,
      params.toJavaMap,
      sessionQueryMapper)
    return if (records.length == 1) UserSessionIsVaild() else UserSessionIsNotVaild();
  }

  private def sessionQueryMapper(
    results: ArrayBuffer[Session],
    record: java.util.Map[java.lang.String, Object]) = {
    val sessionId = mapString("sessionId", record, true)
    val sessionTime = mapLong("issuedTime", record, true)
    results += Session(sessionId, sessionTime)
  }
}
