package org.machine.engine.flow

class EngineMessageBase(
  val id:String,
  val status:String,
  val messageType: String,
  val textMessage:String
) extends EngineMessage{}
