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
  implicit val createUserFormat = jsonFormat4(CreateUser)
  implicit val newUserFormat = jsonFormat1(NewUser)
}

case class CreateUser(emailAddress:String, firstName: String, lastName:String, userName: String)
case class NewUser(userId: String)

case class CreateNewUserRequest(createUser: CreateUser)
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
    val user = request.createUser
    val userId = Engine.getInstance
      .createUser
      .withFirstName(user.firstName)
      .withLastName(user.lastName)
      .withEmailAddress(user.emailAddress)
      .withUserName(user.userName)
    .end
    return NewUser(userId)
  }
}
