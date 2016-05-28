package org.machine.engine.graph.decisions

trait Node{
  def id_= (identifier: Short):Unit
  def id:Short
  def name:String
  def children:Seq[Node]
  def typeStr:String
}
