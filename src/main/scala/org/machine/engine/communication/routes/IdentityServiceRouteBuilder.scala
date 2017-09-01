package org.machine.engine.communication.routes
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
import akka.http.scaladsl.server.directives.Credentials
import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import com.softwaremill.session.SessionResult._
import com.typesafe.scalalogging.{LazyLogging}

import java.util.Optional;

import org.machine.engine.authentication.PasswordTools
import org.machine.engine.communication.headers.UserSession
import org.machine.engine.communication.services.{UserServiceActor, CreateUser,
  NewUser, CreateNewUserRequest, LoginRequest, LoginResponse, LoginUserServiceJsonSupport,
  LoginUserServiceActor,NewUserResponse, UserLoginResponse, UserServiceJsonSupport,
  SessionServiceJsonSupport, SaveUserSessionRequest, SaveUserSessionResponse,
  SessionServiceActor, LogOutUserSessionRequest, LogOutUserSessionResponse};
import org.machine.engine.graph.Neo4JHelper

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure};

object IdentityServiceRouteBuilder extends Directives
  with UserServiceJsonSupport
  with LoginUserServiceJsonSupport
  with SessionServiceJsonSupport
  with LazyLogging{
  import PasswordTools._;

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val config = system.settings.config
  private var actorCounter:Integer = 0

  private val sessionSecret = config.getString("engine.communication.identity_service.session.secret")
  private val sessionConfig = SessionConfig.default(sessionSecret)
  implicit val serializer = JValueSessionSerializer.caseClass[UserSession]
  implicit val encoder = new JwtSessionEncoder[UserSession]
  implicit val sessionManager = new SessionManager(sessionConfig)
  private val clientSessionManager = sessionManager.clientSessionManager

  private val userService = system.actorOf(UserServiceActor.props(), generateNewServiceName("user"))
  private val loginService = system.actorOf(LoginUserServiceActor.props(), generateNewServiceName("login"))
  private val sessionService = system.actorOf(SessionServiceActor.props(), generateNewServiceName("session"))

  private val SESSION_BYTE_SIZE = 128
  private val GENERAL_TIME_OUT = 5 seconds;

  private var generator = createRandomNumberGenerator(generateSeed())
  private val SESSION_REQUEST_HEADER = config.getString("akka.http.session.header.get-from-client-name")
  private val SESSION_RESPONSE_HEADER = config.getString("akka.http.session.header.send-to-client-name")

  private def generateNewServiceName(actorName: String):String = {
    actorCounter = actorCounter + 1
    return s"identity_service-actor-${actorName}-${actorCounter}"
  }

  private def createSessionId():String = {
    val sessionIdBytes = generateSessionId(generator, SESSION_BYTE_SIZE)
    return byteArrayToHexStr(sessionIdBytes)
  }
  /*
  Next Steps:
  1. [X] - Update UserServiceActor to hash and save the user password.
  2. [X] - Create an Actor under the services package to verify the user's password.
  3. [X] - Update this function to call the new Actor to query the DB to find the password.
  4. [X] - Update the Swagger doc to detail the error case.
  5. [X] - Return a session on Login, otherwise return an error.
  6  [ ] - Save the session to the graph.
  7. [ ] - Look up the session in the graph when a request comes in.
  8. [ ] - Update the diagrams with the correct sequence of commands.
  9. [ ] - Write a markdown document detailing how the authentication works, including images.
  10. [ ] - Put a unique constraint on User.userName.
    CREATE CONSTRAINT ON (u:user) ASSERT u.user_name IS UNIQUE
    This should occure only once when the engine starts up. Engine.initializeDatabase()
    https://neo4j.com/docs/developer-manual/current/cypher/schema/constraints/
  11. [ ] - Put an index on the User.userName.
  CREATE INDEX ON :user(user_name)

  I should put an index an all the nodes mid's.
  10. [ ] - Implement logout. (New Actor)
  The JWT looks like it's signed. Dig into the session framework and see how the signiture is being done.
  It would be good to verify the signature of the token if it doesn't do it automatically.
  */
  def buildRoutes():Route = {
    val routes = {
      /*
        Basic Auth is used from a service account perspective.
        User authentication is provided in the body of the requests.
      */
      authenticateBasic(realm = "Engine User Service", authenticator){ user =>
        authorize(hasRights(user)){
          // Just for testing. Remove after the API is done. - SDH
          path("users"){
            get{
              complete(StatusCodes.OK, "Hello World\n")
            }~
            // Create a new user.
            post{
              entity(as[CreateUser]){ newUserRequest =>
                onSuccess(userService.ask(CreateNewUserRequest(newUserRequest))(GENERAL_TIME_OUT)){
                  case response: NewUserResponse => {
                    complete(StatusCodes.OK, response.newUser)
                  }
                  case _ => {
                    complete(StatusCodes.InternalServerError)
                  }
                }
              }
            }
          }~
          // Authenticate a user and return a session ID.
          path("login"){
            post{
              entity(as[LoginRequest]){ request =>
                onSuccess(loginService.ask(request)(GENERAL_TIME_OUT)){
                  case response: LoginResponse  => {
                    response.status match {
                      case 200  => {
                        saveSessionRoute{
                          setSession(oneOff, usingHeaders,
                            UserSession(request.userName,
                              response.userId,
                              createSessionId(),
                              Neo4JHelper.time)){
                            complete(StatusCodes.OK, UserLoginResponse(response.userId))
                          }
                        }
                      }
                      case 401  => {
                        complete(StatusCodes.Unauthorized)
                      }
                    }
                  }
                  case _ => {
                    complete(StatusCodes.InternalServerError)
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
                    logoutTheUser(session)
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
          }~
          path("protected"){
            get{
              requiredSession(oneOff, usingHeaders){ session =>
                complete(StatusCodes.OK)
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
  def saveSessionRoute(innerRoutes: => Route): Route = mapResponse(saveActiveUserSession)(innerRoutes);

  /*
  Communicates with an Akka Actor that saves the session associated with the user.
  */
  private def saveActiveUserSession(response: HttpResponse): HttpResponse = {
    val sessionHeader:Optional[HttpHeader] = response.getHeader(SESSION_RESPONSE_HEADER)
    if(sessionHeader.isPresent()){
      //Assume's that the string is of the format: Header Name + Space + Encoded JWT Token
      val sessionTokenStr:String = sessionHeader.get().toString();
      val tokens = sessionTokenStr.split(" ")
      val decodeAttempt:SessionResult[UserSession] = clientSessionManager.decode(tokens.last);
      /*
      TODO: The error conditions should not just return the response but rather
      force the service to return a 500 or something...
      */
      (decodeAttempt: @unchecked) match {
        case Decoded(session) => {
          logger.debug("Successfully decoded the session header.")
          brokerSavingUserSession(session)
        }
        case CreatedFromToken(session) => {
          logger.error("Decoding the session header incorrectly resulted in a CreatedFromToken object.")
        }
        case NoSession => {
          logger.error("There was no session present on the header.")
        }
        case TokenNotFound => {
          logger.error("There was no token on the session header.")
        }
        case Expired => {
          logger.error("The user's token is expired.")
        }
        case Corrupt(exc) => {
          logger.error("The user's session token is corrupt.")
        }
      }
    }
    return response;
  }

  /*
  Save the user's session via an actor.
  */
  private def brokerSavingUserSession(session: UserSession) = {
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
    Await.result(response, 6 seconds)
  }

  private def logoutTheUser(sessionToken: String) = {
    //The token may or may not have "Bearer before it.
    val tokens = sessionToken.split(" ")
    val decodeAttempt:SessionResult[UserSession] = clientSessionManager.decode(tokens.last);
    (decodeAttempt: @unchecked) match {
      case Decoded(session) => {
        logger.debug("Successfully decoded the session header.")
        brokerlogingOutTheUser(session.userId)
      }
      case Expired => {
        logger.debug("The user's token is expired.")
      }
      case Corrupt(exc) => {
        logger.error("The user's session token is corrupt.")
      }
      case _ => logger.error("An unexpected response occured when attempting to decode the token.")
    }
  }

  /*
  Log the user out by deleting all of their session verticies through an actor.
  */
  private def brokerlogingOutTheUser(userId: String) = {
    val response = sessionService.ask(LogOutUserSessionRequest(userId))(GENERAL_TIME_OUT)
    response.onSuccess{
      case _ => logger.debug("The session actor responded.")
    }
    response.onFailure{
      case _ => logger.error("The session actor did not respond when attempting to save the user's session.")
    }
    Await.result(response, 6 seconds)
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
}
