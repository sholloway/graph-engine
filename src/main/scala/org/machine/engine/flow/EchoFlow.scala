package org.machine.engine.flow

import akka.http.scaladsl.model.ws.{Message}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow}

object EchoFlow{
  def flow(): Flow[Message, Message, Any] =
    // needed because a noop flow hasn't any buffer that would start processing in tests
    Flow[Message].buffer(1, OverflowStrategy.backpressure)
}
