package org.machine.engine.communication.headers

import com.softwaremill.session.{SessionSerializer, MultiValueSessionSerializer}
import org.json4s._
import scala.util.{Success, Failure, Try}

/*
TODO: Add issue time, if it's not automatically injected.
*/
case class UserSession(userName: String, userId: String)
