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

trait SessionServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val saveSessionRequestFormat = jsonFormat3(SaveUserSessionRequest)
  implicit val saveSessionResponseFormat = jsonFormat1(SaveUserSessionResponse)
}

case class SaveUserSessionRequest(userId: String,
  sessionId: String,
  issuedTime: Long)

case class SaveUserSessionResponse(id:String)

object SessionServiceActor {
  def props(): Props = {
    Props(classOf[SessionServiceActor])
  }
}

class SessionServiceActor extends Actor with ActorLogging{
  import Neo4JHelper._
  def receive = {
    case request: SaveUserSessionRequest => {
      sender() ! saveUserSession(request)
    }
  }

  private def saveUserSession(request: SaveUserSessionRequest):SaveUserSessionResponse = {
    println("saveUserSession")
    println(request)
    return new SaveUserSessionResponse("More blah")
  }
}
