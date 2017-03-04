package org.machine.engine.communication.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.stream.{ActorMaterializer}
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import org.machine.engine.communication.services.{UserServiceActor, CreateUser, NewUser}

object IdentityServiceRouteBuilder{
  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  def buildRoutes():Route = {
    val userService = system.actorOf(UserServiceActor.props(), "userService")
    val routes = {
      /*
       The ask operation (? symbol) involves creating an internal actor for handling
       reply, which needs to have a timeout after which it is destroyed in
       order not to leak resources.
      */
      implicit val timeout = Timeout(5.seconds)

      path("users"){
        get{
          complete(StatusCodes.OK, "Hello World\n")
        }~
        post{
          onSuccess(userService ? CreateUser){
            case response: NewUser => {
              complete(StatusCodes.OK,s"${response.userId}\n")
            }
            case _ => {
              complete(StatusCodes.InternalServerError)
            }
          }
        }
      }
      // path("users"){
      //   post{
      //     entity(as[String]){ payload =>
      //       complete(payload)
      //     }
      //   }
      // }
    }
    return routes;
  }
}

// case class NewUserRequest()
