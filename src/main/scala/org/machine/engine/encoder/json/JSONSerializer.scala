package org.machine.engine.encoder.json

trait JSONSerializer[T]{
  def serialize(results: Seq[T]): String
}
