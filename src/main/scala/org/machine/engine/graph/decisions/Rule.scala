package org.machine.engine.graph.decisions

import net.liftweb.json._
import net.liftweb.json.Serialization.{read, write}

case class Rule(name: String,
  description: String,
  scope: String,
  entityType: String,
  actionType: String,
  filter: String,
  decision: String){
    override def toString = {
      s"$name - $description"
    }
  }

object Rule{
  implicit val formats = Serialization.formats(NoTypeHints)
  def fromJSON(msg: String):Rule = {
    return read[Rule](msg)
  }

  def toJSON(rm: Rule):String = {
    return write(rm)
  }
}
