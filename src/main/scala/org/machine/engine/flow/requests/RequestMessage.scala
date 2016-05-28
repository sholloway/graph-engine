package org.machine.engine.flow.requests

case class RequestMessage(user: String,
  actionType: String,
  scope: String,
  entityType: String,
  filter: String)

import net.liftweb.json._
import net.liftweb.json.Serialization.{read, write}

object RequestMessage{
  implicit val formats = Serialization.formats(NoTypeHints)
  def fromJSON(msg: String):RequestMessage = {
    return read[RequestMessage](msg)
  }

  def toJSON(rm: RequestMessage):String = {
    return write(rm)
  }
}
