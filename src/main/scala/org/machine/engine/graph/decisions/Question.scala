package org.machine.engine.graph.decisions

import org.machine.engine.exceptions._
import scala.collection.mutable

//Questions can only have options.
//Options have a single question or decisions.
class Question(at: String) extends Node{
  private val attribute = at
  private val options = mutable.Map[String, Opt]()
  private var identifier:Option[Short] = None

  def id_= (identifier: Short):Unit = this.identifier = Some(identifier)

  def id:Short = {
    return this.identifier.getOrElse(throw new InternalErrorException("Identifier not set for node."));
  }

  def ~>(option: Opt):Opt = {
    options += (option.name -> option)
    return option
  }

  /** Finds a registered option or registers it.
  */
  def getOrElseUpdate(optionName: String):Opt = {
    val option = Opt(optionName, optionName)
    option.id = NodeIdentityGenerator.id
    return options.getOrElseUpdate(optionName, option)
  }

  /*
  FIXME This could blow up if the request is null or options returns null.
  */
  def evaluate(request: Map[String, Option[String]]):Opt = {
    val requestValue = request(attribute).getOrElse(throw new InternalErrorException("Bad..."))
    return options(requestValue)
  }

  override def toString:String ={
    s"""
    |Question:
    |Attribute: $attribute
    |Options: ${options.mkString(" ")}
    """.stripMargin.replaceAll("\t","")
  }

  def children:Seq[Node] = {
    options.values.toSeq
  }

  def name:String = attribute

  def typeStr:String = "question"
}

object Question{
  def apply(attribute: String):Question = {
    return new Question(attribute)
  }
}
