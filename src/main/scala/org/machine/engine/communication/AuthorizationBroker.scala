package org.machine.engine.communication

import akka.actor.ActorSystem
import akka.http.javadsl.model.HttpHeader;
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.{ActorMaterializer}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Directive1}
import akka.http.scaladsl.server.directives.Credentials
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._
import com.typesafe.scalalogging.{LazyLogging}

import java.util.Optional;

import org.machine.engine.authentication.PasswordTools
import org.machine.engine.communication.headers.UserSession
import org.machine.engine.communication.services._
import org.machine.engine.graph.Neo4JHelper

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure};

object SessionBroker{
  private var broker:Option[SessionBroker] = None

  def getInstance: SessionBroker = {
    if (broker.isEmpty){
      broker = Some(new SessionBroker())
    }
    return broker.get
  }
}

class SessionBroker private() extends UserServiceJsonSupport
  with LoginUserServiceJsonSupport
  with SessionServiceJsonSupport
  with LazyLogging{
  import PasswordTools._;

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val config = system.settings.config
  private val sessionSecret = config.getString("engine.communication.identity_service.session.secret")
  private val sessionConfig = SessionConfig.default(sessionSecret)

  implicit val serializer = JValueSessionSerializer.caseClass[UserSession]
  implicit val encoder = new JwtSessionEncoder[UserSession]
  implicit val sessionManager = new SessionManager(sessionConfig)
  private val clientSessionManager = sessionManager.clientSessionManager

  private var actorCounter:Integer = 0
  private val userService = system.actorOf(UserServiceActor.props(), generateNewServiceName("user"))
  private val loginService = system.actorOf(LoginUserServiceActor.props(), generateNewServiceName("login"))
  private val sessionService = system.actorOf(SessionServiceActor.props(), generateNewServiceName("session"))

  private val SESSION_BYTE_SIZE = 128
  private val GENERAL_TIME_OUT = 5 seconds
  private val GENERAL_WAIT_TIME = 6 seconds

  private var generator = createRandomNumberGenerator(generateSeed())
  private val VALID_SESSION:Boolean = true
  private val INVALID_SESSION:Boolean = false

  def sessionManagerInstance():SessionManager[UserSession] = {
    return this.sessionManager
  }

  /*
  Via the SessionServiceActor find out if the session is recorded in the graph.
  To be considered valid, the session must:
  1. Be attached to the User's crediental.
  2. Have the corrisponding session ID.
  3. Have a matching issuedTime.

  Note: The signature of the JWT and expiration time are enforced by the
  akka-http-session middleware so we don't explicitly check them.
  */
  def verifyTheTokenExists(session: UserSession):Boolean = {
    val response = sessionService.ask(
      IsUserSessionValidRequest(session.userId,
        session.sessionId,
        session.issuedTime))(GENERAL_TIME_OUT)
    val responseValue = Await.result(response, GENERAL_WAIT_TIME)
    val sessionValid:Boolean = responseValue match{
      case r:UserSessionIsVaild => {
        logger.debug("The user session was deemed valid.")
        VALID_SESSION
      }
      case r:UserSessionIsNotVaild => {
        logger.debug("The user session was deemed invalid.")
        INVALID_SESSION
      }
      case unknown => {
        logger.debug("An unexpected response was sent by the SessionActor..")
        INVALID_SESSION
      }
    }
    return sessionValid
  }

  def decodeToken(token: String):SessionResult[UserSession] = clientSessionManager.decode(token)

  def createSessionId():String = {
    val sessionIdBytes = generateSessionId(generator, SESSION_BYTE_SIZE)
    return byteArrayToHexStr(sessionIdBytes)
  }

  def brokerCreatingNewUser(newUserRequest: CreateUser):CreateUserResponse = {
    val futureResponse = userService.ask(CreateNewUserRequest(newUserRequest))(GENERAL_TIME_OUT)
    futureResponse.onSuccess{
      case _ => logger.debug("The user actor responded.")
    }
    futureResponse.onFailure{
      case _ => logger.error("The user actor did not respond when attempting to create a new user.")
    }
    return Await.result(futureResponse, GENERAL_WAIT_TIME).asInstanceOf[CreateUserResponse]
  }

  def brokerUserLogin(request: LoginRequest):LoginResponse = {
    val futureResponse = loginService.ask(request)(GENERAL_TIME_OUT)
    futureResponse.onSuccess{
      case _ => logger.debug("The login actor responded.")
    }
    futureResponse.onFailure{
      case _ => logger.error("The login actor did not respond when attempting to login a user.")
    }
    return Await.result(futureResponse, GENERAL_WAIT_TIME).asInstanceOf[LoginResponse]
  }

  /*
  Save the user's session via an actor.
  */
  def brokerSavingUserSession(session: UserSession) = {
    val response = sessionService.ask(
      SaveUserSessionRequest(session.userId,
        session.sessionId,
        session.issuedTime))(GENERAL_TIME_OUT)
    response.onSuccess{
      case _ => logger.debug("The session actor responded.")
    }
    response.onFailure{
      case _ => logger.error("The session actor did not respond when attempting to save the user's session.")
    }
    Await.result(response, GENERAL_WAIT_TIME)
  }

  /*
  Log the user out by deleting all of their session verticies through an actor.
  */
  def brokerloggingOutTheUser(userId: String) = {
    val response = sessionService.ask(LogOutUserSessionRequest(userId))(GENERAL_TIME_OUT)
    response.onSuccess{
      case _ => logger.debug("The session actor responded.")
    }
    response.onFailure{
      case _ => logger.error("The session actor did not respond when attempting to save the user's session.")
    }
    Await.result(response, GENERAL_WAIT_TIME)
  }

  private def generateNewServiceName(actorName: String):String = {
    actorCounter = actorCounter + 1
    return s"identity_service-actor-${actorName}-${actorCounter}"
  }
}
