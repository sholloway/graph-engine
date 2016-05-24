package org.machine.engine.graph.decisions

import org.machine.engine.exceptions._
import org.machine.engine.graph.commands._

object DecisionDSL{
  def findDecision(question: Question, request: DecisionRequest, trace:Boolean = false):Decision = {
    if(trace){Console.println(question)}
    val option = question.evaluate(request.toMap)

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

  import scala.collection.mutable
  def buildDecisionTreeFromRules(dir: String):Question = {
    val scope = Question("scope")
    val rules = loadRules(dir)

    rules.foreach{ rule =>
      registerRule(scope, rule)
    }

    //Console.println(rules.mkString("\n"))
    return scope
  }

  /*
  Order of operations
  Scope? -> Option -> entityType? -> Option -> actionType? -> Option -> filter? -> Option -> Decision

  First pass. Be explicit.
  */
  private def registerRule(scope: Question, rule: Rule) = {
    //Does the scope option exist? If not create it
    val scopeOption = scope.getOrElseUpdate(rule.scope)

    //Does the EntityType option exist? If not create it
    val entityQuestion = scopeOption.getOrElseUpdateQuestion("entityType")
    val entityOption = entityQuestion.getOrElseUpdate(rule.entityType)

    //Does the ActionType option exist? If not create it
    val actionTypeQuestion = entityOption.getOrElseUpdateQuestion("actionType")
    val actionTypeOption = actionTypeQuestion.getOrElseUpdate(rule.actionType)

    //Does the FilterType option exist? If not create it
    val filterQuestion = actionTypeOption.getOrElseUpdateQuestion("filter")
    val filterOption = filterQuestion.getOrElseUpdate(rule.filter)

    //Register Decision
    filterOption -> Decision(rule.decision)
  }

  private def loadRules(dir:String):Seq[Rule] = {
    val files = getListOfFiles(dir, List("json"))
    val rules = mutable.ArrayBuffer.empty[Rule]
    files.foreach(file => {
      val json:Option[String] = readFileToString(file)
      json.foreach{
        rules += Rule.fromJSON(_)
      }
    })
    return rules
  }

  import java.io.File
  private def getListOfFiles(path: String, extensions: List[String]):List[File] = {
    // val path = getClass.getClassLoader.getResource(dir).getPath
    val d = new File(path)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList.filter{ file =>
        extensions.exists(file.getName.endsWith(_))
      }
    } else {
      List[File]()
    }
  }

  import scala.io.Source
  private def readFileToString(file: File):Option[String] = {
    val bufferedFile = Source.fromFile(file)
    var contents:Option[String] = None
    try{
      contents = Some(bufferedFile.getLines.mkString)
    }finally{
      bufferedFile.close
    }
    return contents
  }

  def drawTree(node: Node, depth: Int, plotter: Plotter):Unit = {
    plotter.plot(node, depth)
    if(node.children.isEmpty){
      return
    }
    node.children.foreach(child => drawTree(child, depth + 1, plotter))
  }

  /*
  Create a GraphViz Dot file for the directed graph.
  The syntax is of the format:
  digraph{
  	a->{b c d e f g}
  	b->{e f}[color="blue"]
  }
  */
  import scala.collection.mutable
  def createDotFile(node:Node):String = {
    val adjacencyList = mutable.Map.empty[String, Seq[String]]
    breadthFirstTraverse(node, adjacencyList)

    val graph = mutable.ArrayBuffer.empty[String]
    adjacencyList.foreach{ case (node, children) => { graph += s"$node->{${children.mkString(" ")}}"}}
    val dot = s"""
      |digraph EngineDecisionTree{
      |\t${graph.mkString("\n\t")}
      |}
      """.stripMargin
    return dot
  }

  def breadthFirstTraverse(node:Node, edges: mutable.Map[String, Seq[String]]):Unit = {
    node match{
      case q: Question => {
        val nodeEdges = node.children.map(_.name).toSeq
        edges += (node.name -> nodeEdges)
        node.children.foreach(breadthFirstTraverse(_, edges))
      }
      case o: Opt => {
        val nodeEdges = node.children.map(_.name).toSeq
        edges += (node.name -> nodeEdges)
        node.children.foreach(breadthFirstTraverse(_, edges))
      }
      case d: Decision => return
    }
  }
}

trait Plotter{
  def plot(node:Node, depth: Int)
}

class ConsolePlotter extends Plotter{
  def plot(node:Node, depth: Int) = {
    val indented = if (depth > 1){
       "    " * (depth-1)
    }else{
      ""
    }
    val bar = if(depth > 0) "└-- " else ""
    val line = s"$indented$bar${node.name}.${node.typeStr}:"
    Console.println(line)
  }
}
