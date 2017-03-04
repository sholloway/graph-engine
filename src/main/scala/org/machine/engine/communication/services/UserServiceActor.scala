package org.machine.engine.communication.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.{LazyLogging}

case class CreateUser(emailAddress:String, firstName: String, lastName:String, userName: String)
case class NewUser(userId: String)

object UserServiceActor {
  def props(): Props = {
    Props(classOf[UserServiceActor])
  }
}

class UserServiceActor extends Actor with ActorLogging{
  def receive = {
    case CreateUser => {
      sender() ! NewUser("abc")
    }
  }
}
