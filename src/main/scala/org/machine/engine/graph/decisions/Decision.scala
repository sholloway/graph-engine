package org.machine.engine.graph.decisions

import org.machine.engine.exceptions.InternalErrorException

case class Decision(val name: String) extends Node{
  private var identifier:Option[Short] = None

  def id_= (identifier: Short):Unit = this.identifier = Some(identifier)

  def id:Short = {
    return this.identifier.getOrElse(throw new InternalErrorException("Identifier not set for node."));
  }

  def children:Seq[Node] = {
    Seq.empty[Node]
  }

  def typeStr:String = "decision"
}
