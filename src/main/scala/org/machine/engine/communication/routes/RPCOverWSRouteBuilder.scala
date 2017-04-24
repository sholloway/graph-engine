package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.Route
import org.machine.engine.flow.{EchoFlow, WebSocketFlow}

object RPCOverWSRouteBuilder extends Directives {
  private implicit val system = ActorSystem()
  private val config = system.settings.config
  private val USER_NAME = "engine.communication.webserver.user"
  private val PASSWORD = "engine.communication.webserver.password"
  private val JSON_PROTOCOL = "engine.json.v1"

  def buildRoutes():Route = {
    val routes = {
      authenticateBasic(realm = "Engine User Service", authenticator){ user =>
        authorize(hasRights(user)){
          path("ws"){
            handleWebSocketMessagesForProtocol(WebSocketFlow.flow, JSON_PROTOCOL)
          }~
          path("ws" / "ping"){
            handleWebSocketMessagesForProtocol(EchoFlow.flow, JSON_PROTOCOL)
          }
        }
      }
    }
    return routes
  }

  private def authenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(id) if p.verify(config.getString(PASSWORD)) => Some(id)
      case _ => None
    }

  private def hasRights(user: String):Boolean  = {
    val registeredUser = config.getString(USER_NAME)
    return registeredUser == user
  }
}
