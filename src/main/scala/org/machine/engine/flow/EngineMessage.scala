package org.machine.engine.flow

/**
An immutable message initiated by the Engine to a client.
*/
trait EngineMessage{
  def id: String
  def status: String
  def messageType: String
  def textMessage: String
  override def toString:String = {
    s"""
    |id: $id  status: $status
    |Text Message:
    |$textMessage
    """.stripMargin
  }
}
