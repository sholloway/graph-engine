package org.machine.engine.communication.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.{LazyLogging}

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

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
      sender() ! NewUserResponse(NewUser(request.createUser.userName))
    }
  }
}
