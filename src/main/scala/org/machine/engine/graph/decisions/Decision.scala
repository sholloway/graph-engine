package org.machine.engine.graph.decisions

case class Decision(val name: String) extends Node{
  def children:Seq[Node] = {
    Seq.empty[Node]
  }

  def typeStr:String = "decision"
}
