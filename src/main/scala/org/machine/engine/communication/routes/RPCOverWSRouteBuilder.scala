package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.Route
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._
import com.typesafe.scalalogging.LazyLogging

import org.machine.engine.communication.SessionBroker
import org.machine.engine.communication.headers.UserSession
import org.machine.engine.communication.services._
import org.machine.engine.flow.{EchoFlow, WebSocketFlow}

object RPCOverWSRouteBuilder extends Directives with LazyLogging{
  private implicit val system = ActorSystem()
  private val config = system.settings.config
  private val SESSION_HEADER:String = config.getString("engine.communication.webserver.session.header")
  private val USER_NAME = "engine.communication.webserver.user"
  private val PASSWORD = "engine.communication.webserver.password"
  private val JSON_PROTOCOL = "engine.json.v1"
  private val sessionBroker:SessionBroker = SessionBroker.getInstance

  def buildRoutes():Route = {
    val routes = {
      authenticateBasic(realm = "Engine User Service", authenticator){ user =>
        authorize(hasRights(user)){
          path("ws"){
            handleWebSocketMessagesForProtocol(WebSocketFlow.flow, JSON_PROTOCOL)
          }~
          path("ws" / "ping"){
            //Return a 400 if the Session header is not present.
            headerValueByName(SESSION_HEADER){ session =>
              requiredActiveSession(session){ userSession =>
                println(userSession)
                handleWebSocketMessagesForProtocol(EchoFlow.flow, JSON_PROTOCOL)
              }
            }
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

  private def requiredActiveSession[T](sessionToken: String): Directive1[UserSession] = {
    //Support the format of the token having Bearer or not.
    val tokens = sessionToken.split(" ")
    val decodeAttempt:SessionResult[UserSession] = sessionBroker.decodeToken(tokens.last);
    (decodeAttempt: @unchecked) match {
      case Decoded(session) => {
        logger.debug("Successfully decoded the session header.")
        //Note: The akka-http-session framework is enforcing the token expiration.
        //So we're not checking it ourselves.
        sessionBroker.verifyTheTokenExists(session) match {
          case true => provide(session)
          case false => complete(StatusCodes.Unauthorized)
        }
      }
      case Expired => {
        logger.debug("The user's token is expired.")
        complete(StatusCodes.Unauthorized)
      }
      case Corrupt(exc) => {
        logger.error("The user's session token is corrupt.")
        complete(StatusCodes.Unauthorized)
      }
      case _ => {
        logger.error("An unexpected response occured when attempting to decode the token.")
        complete(StatusCodes.Unauthorized)
      }
    }
  }
}
