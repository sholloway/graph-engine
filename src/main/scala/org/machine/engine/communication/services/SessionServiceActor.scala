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
}

case class SaveUserSessionRequest(userId: String,
  sessionId: String,
  issuedTime: Long)

case class SaveUserSessionResponse()

object SessionServiceActor {
  def props(): Props = {
    Props(classOf[SessionServiceActor])
  }
}

class SessionServiceActor extends Actor with ActorLogging{
  import Neo4JHelper._

  //Create (user)-[:authenticates_with]->(credential)-[:is_logged_in_with]->(session)
  private val saveUserSessionStmt: String = """
  |match(u:user)-[:authenticates_with]->(c:credential) where u.mid = {userId}
  |create (c)-[:is_logged_in_with]->(s:session {
  | session:{sessionId},
  | creation_time: {issuedTime}
  |})
  """.stripMargin

  def receive = {
    case request: SaveUserSessionRequest => {
      sender() ! saveUserSession(request)
    }
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
}
