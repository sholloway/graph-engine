package org.machine.engine.flow

/**
An immutable message issued by a remote client.
*/
trait ClientMessage{
  def payload: String
  def time: Long //When the request was issued
}
