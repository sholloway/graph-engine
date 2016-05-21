package org.machine.engine.graph.decisions

import org.machine.engine.exceptions._
import org.machine.engine.graph.commands._


object DecisionDSL{
  def findDecision(question: Question, request: Map[String, Option[String]], trace:Boolean = false):Decision = {
    if(trace){Console.println(question)}
    val option = question.evaluate(request)

    /*
    FIXME Could result in no return.
    */
    if (!option.decision.isEmpty){
      return option.decision.get
    }else if(!option.question.isEmpty){
      return findDecision(option.question.getOrElse(throw new InternalErrorException("No Question Mounted")), request)
    }else{
      throw new InternalErrorException("Incomplete Decision Tree")
    }
  }

  def buildDecisionTree():Question = {
    val whatsTheScope       = Question("scope")
    val userSpace           = Opt(CommandScopes.UserSpaceScope.scope)
    val whatsTheEntityType  = Question("entityType")
    val dataSet             = Opt("DataSet")
    val whatsTheActionType  = Question("actionType")
    val retrieve            = Opt("retrieve")

    val whatsTheFilter = Question("filter")
    whatsTheFilter ~> Opt("All") -> Decision("ListDataSets")
    whatsTheFilter ~> Opt("ID") -> Decision("FindDataSetById")
    whatsTheFilter ~> Opt("Name") -> Decision("FindDataSetByName")

    whatsTheScope ~> userSpace ~> whatsTheEntityType ~> dataSet ~> whatsTheActionType ~> retrieve ~> whatsTheFilter
    return whatsTheScope
  }
}
