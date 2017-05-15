package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.{ActorMaterializer}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.Credentials

import org.machine.engine.communication.headers.UserSession
import org.machine.engine.communication.services.{UserServiceActor, CreateUser,
  NewUser, CreateNewUserRequest, LoginRequest, LoginResponse, LoginUserServiceJsonSupport,
  LoginUserServiceActor,NewUserResponse, UserLoginResponse, UserServiceJsonSupport};

import com.softwaremill.session._
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._

object IdentityServiceRouteBuilder extends Directives
  with UserServiceJsonSupport
  with LoginUserServiceJsonSupport{
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val config = system.settings.config
  private var userServiceCounter:Integer = 0
  private var loginServiceCounter:Integer = 0

  val sessionSecret = config.getString("engine.communication.identity_service.session.secret")
  val sessionConfig = SessionConfig. default(sessionSecret)
  implicit val serializer = JValueSessionSerializer.caseClass[UserSession]
  implicit val encoder = new JwtSessionEncoder[UserSession]
  implicit val sessionManager = new SessionManager(sessionConfig)

  private def generateNewUserServiceName():String = {
    userServiceCounter = userServiceCounter + 1
    return s"identity-user-$userServiceCounter"
  }

  private def generateNewLoginServiceName():String = {
    loginServiceCounter = loginServiceCounter + 1
    return s"identity-login-$loginServiceCounter"
  }

  def buildRoutes():Route = {
    val userService = system.actorOf(UserServiceActor.props(), generateNewUserServiceName())
    val loginService = system.actorOf(LoginUserServiceActor.props(), generateNewLoginServiceName())

    val routes = {
      /*
       The ask operation (? symbol) involves creating an internal actor for handling
       reply, which needs to have a timeout after which it is destroyed in
       order not to leak resources.
      */
      implicit val timeout = Timeout(5.seconds)

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
                onSuccess(userService ? CreateNewUserRequest(newUserRequest)){
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
                onSuccess(loginService ? request){
                  case response: LoginResponse  => {
                    /*
                    I need to handle response.status 200 and 401.
                    complete(StatusCodes.Unauthorized)
                    */
                    response.status match {
                      case 200  => {
                        setSession(oneOff, usingHeaders, UserSession(request.userName, response.userId)){
                          /*
                          Next Steps:
                          1. [X] - Update UserServiceActor to hash and save the user password.
                          2. [X] - Create an Actor under the services package to verify the user's password.
                          3. [X] - Update this function to call the new Actor to query the DB to find the password.
                          4. [X] - Update the Swagger doc to detail the error case.
                          5. [X] - Return a session on Login, otherwise return an error.
                          6. [ ] - Update the diagrams with the correct sequence of commands.
                          7. [ ] - Write a markdown document detailing how the authentication works, including images.
                          8. [ ] - Put a unique constraint on User.userName.
                            CREATE CONSTRAINT ON (u:user) ASSERT u.user_name IS UNIQUE
                            This should occure only once when the engine starts up. Engine.initializeDatabase()
                            https://neo4j.com/docs/developer-manual/current/cypher/schema/constraints/
                          9. [ ] - Put an index on the User.userName.
                          CREATE INDEX ON :user(user_name)

                          I should put an index an all the nodes mid's.
                          10. [ ] - Implement logout. (New Actor)
                          The JWT looks like it's signed. Dig into the session framework and see how the signiture is being done.
                          It would be good to verify the signature of the token if it doesn't do it automatically.
                          */
                          complete(StatusCodes.OK, UserLoginResponse(response.userId))
                          // complete(StatusCodes.OK, "Hello. My Name is Inega Montoya.")
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
          }
        }
      }
    }
    return routes;
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
