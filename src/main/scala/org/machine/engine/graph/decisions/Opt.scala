package org.machine.engine.graph.decisions

import scala.collection.mutable
case class Opt(val name: String, val value: String) extends Node{
  var q: Option[Question] = None
  var d: Option[Decision] = None

  def question = q
  def decision = d

  def ~>(question:Question):Question = {
    this.q = Some(question)
    return this.q.get
  }

  def ->(decision:Decision) = {
    this.d = Some(decision)
  }

  def getOrElseUpdateQuestion(qq: String):Question = {
    if (question.isEmpty){
      this ~> Question(qq)
    }
    return question.get
  }

  def getOrElseUpdateDecision(dd: String):Decision = {
    if(decision.isEmpty){
      this -> Decision(dd)
    }
    return decision.get
  }

  override def toString:String = {
    s"name: $name"
  }

  def children:Seq[Node] = {
    val mut = mutable.ArrayBuffer[Node]()
    decision.foreach(mut += _)
    question.foreach(mut += _)
    mut.toSeq
  }

  def typeStr:String = "option"
}

object Opt{
  def apply(name: String):Opt ={
    new Opt(name, name)
  }
}
