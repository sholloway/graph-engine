package org.machine.engine.graph.decisions

case class Opt(val name: String, val value: String){
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

  override def toString:String = {
    s"name: $name"
  }
}

object Opt{
  def apply(name: String):Opt ={
    new Opt(name, name)
  }
}
