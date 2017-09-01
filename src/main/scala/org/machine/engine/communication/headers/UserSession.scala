package org.machine.engine.communication.headers

import com.softwaremill.session.{SessionSerializer, MultiValueSessionSerializer}
import org.json4s._
import scala.util.{Success, Failure, Try}

case class UserSession(userName: String,
  userId: String,
  sessionId: String,
  issuedTime: Long)
