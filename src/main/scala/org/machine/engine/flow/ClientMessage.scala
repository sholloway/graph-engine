package org.machine.engine.flow

import org.machine.engine.graph.Neo4JHelper

/**
An immutable message issued by a remote client.
*/
trait ClientMessage{
  def payload: String
  def time: Long //When the request was issued
}

/**
Base implimentation of the ClientMessage trait.
*/
class ClientMessageBase(requestMsg:String) extends ClientMessage{
  val creationTime = Neo4JHelper.time
  def payload:String = requestMsg
  def time:Long = creationTime
}

object ClientMessage{
  def apply(msg: String):ClientMessage = {
    new ClientMessageBase(msg)
  }
}
