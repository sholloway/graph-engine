package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.{ActorMaterializer}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import org.machine.engine.communication.services.{UserServiceActor, CreateUser,
  NewUser, CreateNewUserRequest, NewUserResponse, UserServiceJsonSupport};
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.Credentials

object IdentityServiceRouteBuilder extends Directives with UserServiceJsonSupport{
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private val config = system.settings.config
  private var userServiceCounter:Integer = 0

  private def generateNewUserServiceName():String = {
    userServiceCounter = userServiceCounter + 1
    return s"identity-user-$userServiceCounter"
  }

  def buildRoutes():Route = {
    val userService = system.actorOf(UserServiceActor.props(), generateNewUserServiceName())
    val routes = {
      /*
       The ask operation (? symbol) involves creating an internal actor for handling
       reply, which needs to have a timeout after which it is destroyed in
       order not to leak resources.
      */
      implicit val timeout = Timeout(5.seconds)

      authenticateBasic(realm = "Engine User Service", authenticator){ user =>
        authorize(hasRights(user)){
          path("users"){
            get{
              complete(StatusCodes.OK, "Hello World\n")
            }~
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

// case class NewUserRequest()
