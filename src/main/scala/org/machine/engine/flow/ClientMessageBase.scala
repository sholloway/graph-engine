package org.machine.engine.flow

import org.machine.engine.graph.Neo4JHelper

/**
Base implimentation of the ClientMessage trait.
*/
class ClientMessageBase(requestMsg:String) extends ClientMessage{
  val creationTime = Neo4JHelper.time
  def payload:String = requestMsg
  def time:Long = creationTime
}
