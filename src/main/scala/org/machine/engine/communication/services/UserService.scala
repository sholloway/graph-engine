package org.machine.engine.communication.services

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.scalalogging.{LazyLogging}

class UserService extends LazyLogging{
  import scala.concurrent.ExecutionContext.Implicits.global
  def process(req: HttpRequest):HttpResponse = {
    return HttpResponse(200, entity = "Registered New User")
  }
}
