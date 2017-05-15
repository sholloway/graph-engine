package org.machine.engine.communication.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.{LazyLogging}
import spray.json._

import org.machine.engine.Engine

trait UserServiceJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val createUserFormat = jsonFormat5(CreateUser)
  implicit val newUserFormat = jsonFormat1(NewUser)
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
  implicit val loginResponseFormat = jsonFormat1(LoginResponse)
}

case class CreateUser(emailAddress:String, firstName: String, lastName:String, userName: String, var password: String)
case class CreateNewUserRequest(createUser: CreateUser)
case class LoginRequest(userName: String, password: String)
case class LoginResponse(userId: String)
case class NewUser(userId: String)
case class NewUserResponse(newUser: NewUser)

object UserServiceActor {
  def props(): Props = {
    Props(classOf[UserServiceActor])
  }
}

class UserServiceActor extends Actor with ActorLogging{
  def receive = {
    case request: CreateNewUserRequest => {
      val newUser = createNewUser(request)
      sender() ! NewUserResponse(newUser)
    }
  }

  private def createNewUser(request: CreateNewUserRequest):NewUser = {
    val userId = Engine.getInstance
      .createUser
      .withFirstName(request.createUser.firstName)
      .withLastName(request.createUser.lastName)
      .withEmailAddress(request.createUser.emailAddress)
      .withUserName(request.createUser.userName)
      .withUserPassword(request.createUser.password)
    .end
    // Remove the user's password from memory immediately.
    request.createUser.password = "REDACTED"
    return NewUser(userId)
  }
}
