package org.machine.engine.encoder.json

import org.machine.engine.graph.commands.EngineCmdResult

trait JSONSerializer[T]{
  def serialize(resultStatus: EngineCmdResult, resultValue: Seq[T]): String
}
