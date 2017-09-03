package org.machine.engine.communication.routes
import akka.http.javadsl.model.HttpHeader;
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{Directive0, Directive1}
import akka.http.scaladsl.server.Route
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._
import com.typesafe.config._
import com.typesafe.scalalogging.{LazyLogging}

import java.util.Optional;

import org.machine.engine.authentication.PasswordTools
import org.machine.engine.communication.SessionBroker
import org.machine.engine.communication.headers.UserSession
import org.machine.engine.communication.services._
import org.machine.engine.graph.Neo4JHelper

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure};

object IdentityServiceRouteBuilder extends Directives
  with LazyLogging
  with LoginUserServiceJsonSupport
  with UserServiceJsonSupport{

  private val config = ConfigFactory.load()
  private val SESSION_REQUEST_HEADER = config.getString("akka.http.session.header.get-from-client-name")
  private val SESSION_RESPONSE_HEADER = config.getString("akka.http.session.header.send-to-client-name")
  private val sessionBroker:SessionBroker = SessionBroker.getInstance
  implicit val sessionManager = sessionBroker.sessionManagerInstance
  /*
  Next Steps:
  00. Clean up the code.
  000. Add ScalaDoc for all classes & methods.

  1. [ ] - Get the WebSocket endpoint using the sessions.
  2. [ ] - Put a unique constraint on User.userName.
    CREATE CONSTRAINT ON (u:user) ASSERT u.user_name IS UNIQUE
    This should occure only once when the engine starts up. Engine.initializeDatabase()
    https://neo4j.com/docs/developer-manual/current/cypher/schema/constraints/
  3. [ ] - Put an index on the User.userName.
           CREATE INDEX ON :user(user_name)
           I should put an index an all the nodes mid's.
  4. [ ] - Update the diagrams with the correct sequence of commands.
  5. [ ] - Write a markdown document detailing how the authentication works, including images.
  */
  def buildRoutes():Route = {
    val routes = {
      /*
        Basic Auth is used from a service account perspective.
        User authentication is provided in the body of the requests.
      */
      authenticateBasic(realm = "Engine User Service", authenticator){ user =>
        authorize(hasRights(user)){
          path("users"){
            /* TODO: Just for testing. Remove after the API is done. - SDH */
            get{
              complete(StatusCodes.OK, "Hello World\n")
            }~
            post{
              entity(as[CreateUser]){ newUserRequest =>
                createNewUser(newUserRequest){ newUser =>
                  complete(StatusCodes.OK, newUser)
                }
              }
            }
          }~
          // Authenticate a user and return a session ID.
          path("login"){
            post{
              entity(as[LoginRequest]){ loginRequest =>
                attemptLogin(loginRequest){ loginResponse =>
                  saveSession{
                    setSession(oneOff, usingHeaders,
                      UserSession(loginRequest.userName,
                        loginResponse.userId,
                        sessionBroker.createSessionId(),
                        Neo4JHelper.time)){
                      complete(StatusCodes.OK, UserLoginResponse(loginResponse.userId))
                    }
                  }
                }
              }
            }
          }~
          path("logout"){
            get{
              requiredSession(oneOff, usingHeaders){ session =>
                invalidateSession(oneOff, usingHeaders){
                  headerValueByName(SESSION_REQUEST_HEADER){ session =>
                    logoutTheUser(session){
                      complete(StatusCodes.OK)
                    }
                  }
                }
              }
            }
          }~
          path("protected"){
            get{
              requiredSession(oneOff, usingHeaders){ session =>
                headerValueByName(SESSION_REQUEST_HEADER){ session =>
                  requiredActiveSession(session) {
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
          }
        }
      }
    }
    return routes;
  }

  /*
  This route will attempt to save the session that was created in the inner route.
  */
  private def saveSession(innerRoutes: => Route): Route = mapResponse(saveActiveUserSession)(innerRoutes);

  /*
  Communicates with an Akka Actor that saves the session associated with the user.
  */
  private def saveActiveUserSession(response: HttpResponse): HttpResponse = {
    val sessionHeader:Optional[HttpHeader] = response.getHeader(SESSION_RESPONSE_HEADER)
    if(sessionHeader.isPresent()){
      //Assume's that the string is of the format: Header Name + Space + Encoded JWT Token
      val sessionTokenStr:String = sessionHeader.get().toString();
      val tokens = sessionTokenStr.split(" ")
      val decodeAttempt:SessionResult[UserSession] = sessionBroker.decodeToken(tokens.last)
      /*
      TODO: The error conditions should not just return the response but rather
      force the service to return a 500 or something...
      */
      (decodeAttempt: @unchecked) match {
        case Decoded(session) => {
          logger.debug("Successfully decoded the session header.")
          sessionBroker.brokerSavingUserSession(session)
        }
        case Expired => {
          logger.error("The user's token is expired.")
        }
        case Corrupt(exc) => {
          logger.error("The user's session token is corrupt.")
        }
        case _ => logger.error("An unexpected response occured when attempting to decode the token.")
      }
    }
    return response;
  }

  private def authenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p @ Credentials.Provided(id) if p.verify(config.getString("engine.communication.identity_service.password")) => Some(id)
      case _ => None
    }

  private def hasRights(user: String):Boolean  = {
    val registeredUser = config.getString("engine.communication.identity_service.user")
    return registeredUser == user
  }

  private def requiredActiveSession[T](sessionToken: String): Directive0 = {
    val tokens = sessionToken.split(" ")
    val decodeAttempt:SessionResult[UserSession] = sessionBroker.decodeToken(tokens.last);
    (decodeAttempt: @unchecked) match {
      case Decoded(session) => {
        logger.debug("Successfully decoded the session header.")
        //Note: The akka-http-session framework is enforcing the token expiration.
        //So we're not checking it ourselves.
        sessionBroker.verifyTheTokenExists(session) match {
          case true => pass
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

  private def createNewUser(newUserRequest: CreateUser):Directive1[NewUser] = {
    sessionBroker.brokerCreatingNewUser(newUserRequest) match{
      case ur: NewUserResponse => provide(ur.newUser)
      case _ => complete(StatusCodes.InternalServerError)
    }
  }

  private def attemptLogin(request: LoginRequest):Directive1[LoginResponse] =  {
    val response = sessionBroker.brokerUserLogin(request)
    response.status match{
      case 200 => provide(response)
      case 401 => complete(StatusCodes.Unauthorized)
      case _ => complete(StatusCodes.InternalServerError)
    }
  }

  private def logoutTheUser(sessionToken: String):Directive0 = {
    //The token may or may not have "Bearer before it.
    val tokens = sessionToken.split(" ")
    val decodeAttempt:SessionResult[UserSession] = sessionBroker.decodeToken(tokens.last);
    (decodeAttempt: @unchecked) match {
      case Decoded(session) => {
        logger.debug("Successfully decoded the session header.")
        sessionBroker.brokerloggingOutTheUser(session.userId)
        pass
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
        pass
      }
    }
  }
}
