package org.machine.engine.graph.decisions

trait Node{
  def name:String
  def children:Seq[Node]
  def typeStr:String
}
