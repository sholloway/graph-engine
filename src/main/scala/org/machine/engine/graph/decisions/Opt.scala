package org.machine.engine.graph.decisions

import scala.collection.mutable
import org.machine.engine.exceptions.InternalErrorException

case class Opt(val name: String, val value: String) extends Node{
  private var q: Option[Question] = None
  private var d: Option[Decision] = None
  private var identifier:Option[Short] = None

  def question = q
  def decision = d

  def id_= (identifier: Short):Unit = this.identifier = Some(identifier)

  def id:Short = {
    return this.identifier.getOrElse(throw new InternalErrorException("Identifier not set for node."));
  }

  def ~>(question:Question):Question = {
    this.q = Some(question)
    return this.q.get
  }

  def ->(decision:Decision) = {
    this.d = Some(decision)
  }

  def getOrElseUpdateQuestion(qq: String):Question = {
    if (question.isEmpty){
      val qqq = Question(qq)
      qqq.id = NodeIdentityGenerator.id
      this ~> qqq
    }
    return question.get
  }

  def getOrElseUpdateDecision(dd: String):Decision = {
    if(decision.isEmpty){
      val ddd = Decision(dd)
      ddd.id = NodeIdentityGenerator.id
      this -> ddd
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
