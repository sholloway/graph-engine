package org.machine.engine.graph.decisions

import org.machine.engine.exceptions._
import org.machine.engine.graph.commands._
import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.{Files, Paths}
import java.util.jar.JarFile
import scala.collection.mutable
import scala.io.Source

object DecisionDSL{
  def findDecision(question: Question, request: DecisionRequest, trace:Boolean = false):Decision = {
    if(trace){Console.println(question)}
    val option = question.evaluate(request.toMap)
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

  def buildDecisionTreeFromRules():Question = {
    val scope = Question("scope")
    scope.id = NodeIdentityGenerator.id
    val rules = loadRules()
    var counter:Short = 0
    rules.foreach{ rule =>
      registerRule(scope, rule, counter)
    }
    return scope
  }

  /*
  Order of operations
  Scope? -> Option -> entityType? -> Option -> actionType? -> Option -> filter? -> Option -> Decision

  First pass. Be explicit.
  */
  private def registerRule(scope: Question, rule: Rule, counter:Short) = {
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
    /*
    NOTE Potential Problem with building Decision Tree
    I could see this approach being an issue. If the option already exists,
    it is possible that another rule already specified a Decision on the
    Option. That isn't logical, since the filter should only have one decision,
    however it could technically happen if there are conflicting rules.
    */
    filterOption.getOrElseUpdateDecision(rule.decision)
  }

  /*
  Redesign to accomidate two ways of loading the files.
  1. Change findRuleFiles to return an array of the loaded files.
  2.
  */
  private def loadRules():Seq[Rule] = {
    val files:Seq[Option[String]] = findRuleFiles()
    val rules = mutable.ArrayBuffer.empty[Rule]
    files.foreach(fileOption => {
      fileOption.foreach{
        rules += Rule.fromJSON(_)
      }
    })
    return rules
  }

  private def findRuleFiles():Seq[Option[String]] = {
    val rulesDir = "org/machine/engine/graph/decisions/rules"
    val jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
    val fsJsonFiles = mutable.ArrayBuffer.empty[Option[String]]
    val loadedFiles:List[Option[String]] = if(jarFile.isFile()) {
      val filePaths:Seq[String] = getListOfFilesFromJar(jarFile, rulesDir)
      val jarJsonFiles = mutable.ArrayBuffer.empty[Option[String]]
      filePaths.foreach(path => {
        fsJsonFiles += readResourceToString(path)
      })
      fsJsonFiles.toList
    } else { // Run with file system. (e.g. Unit Tests)
      val url = getClass.getResource("/"+rulesDir)
      val path = url.getPath()
      val files = getListOfFiles(path, List("json"))
      //Iterate over the files and load them.
      files.foreach(file =>{
        fsJsonFiles += readFileToString(file)
      })
      fsJsonFiles.toList
    }
    return loadedFiles;
  }

  private def getListOfFilesFromJar(jarFile: File, rulesDir: String):Seq[String] = {
    val jar = new JarFile(jarFile);
    val entries = jar.entries()
    val filesAB = mutable.ArrayBuffer.empty[String]
    while(entries.hasMoreElements()) {
      val name = entries.nextElement().getName()
      if (name.startsWith(rulesDir + "/") && name.endsWith("json")) {
        filesAB += name
      }
    }
    jar.close()
    return filesAB.toList
  }

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

  private def readResourceToString(path: String):Option[String] = {
    var json:Option[String] = None;
    try{
      val inputStream: InputStream = getClass().getResourceAsStream("/"+path)
      if(inputStream != null){
        try{
          val str = scala.io.Source.fromInputStream(inputStream).mkString("")
          json = Some(str)
        }catch{
          case e: Exception =>{
            Console.println(s"An error occured attempting to parse the rule: ${path}")
            Console.println(e.getMessage());
            e.printStackTrace()
          }
        }finally{
          inputStream.close()
        }
      }else{
        Console.println(s"Failed to load the resource: ${path}")
      }
    }catch{
      case e: Exception =>{
        Console.println(s"An error occured attempting to parse the rule: ${path}")
        Console.println(e.getMessage());
        e.printStackTrace()
      }
    }
    return json
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
    a [lable="Da Root" color=red]
  }
  */
  import scala.collection.mutable
  case class DotNode(id: Short, label: String, color: String){
    //Used when generating the output.
    override def toString:String = {
      s"$id"
    }

    def display:String = {
      s"""$id [label="$label" color=$color]"""
    }
  }

  def createDotFile(node:Node):String = {
    val adjacencyList = mutable.Map.empty[DotNode, Seq[DotNode]]
    breadthFirstTraverse(node, adjacencyList)

    val graph = mutable.ArrayBuffer.empty[String]
    adjacencyList.foreach{ case (node, children) => {
      graph += s"${node.id}->{${children.mkString(" ")}}"
    }}

    val labels = adjacencyList.keys.map(_.display)
    val dot = s"""
      |digraph EngineDecisionTree{
      |\tgraph [
      |\t\tfontname = "Helvetica",
      |\t\tfontsize = 10,
      |\t\tsplines = true,
      |\t\toverlap = true,
      |\t\tranksep = 2.5,
      |\t\tbgcolor = black
      |\t];
      |\tnode [shape = note,
      |\t\tstyle=filled,
      |\t\tfontname = "Helvetica",
      |\t];
      |\tedge [color = white];
      |\t${graph.mkString("\n\t")}
      |\t${labels.mkString("\n\t")}
      |}
      """.stripMargin
    return dot
  }

  def breadthFirstTraverse(node: Node,
    edges: mutable.Map[DotNode, Seq[DotNode]]):Unit = {
    node match{
      case q: Question => processNode(node, edges)
      case o: Opt => processNode(node, edges)
      case d: Decision => {
        val color = ColorMapper.color("decision")
        val dNode = DotNode(d.id, d.name, color)
        edges += (dNode -> mutable.ArrayBuffer.empty[DotNode])
        return
      }
    }
  }

  private def processNode(node: Node,
    edges: mutable.Map[DotNode, Seq[DotNode]]):Unit ={
    val color = ColorMapper.color(node.name)
    val dotNodes = node.children.map(c => DotNode(c.id, c.name, color))
    val nodeEdges:Seq[DotNode] = dotNodes.toSeq
    val keyNode = DotNode(node.id, node.name, color)
    if(edges.contains(keyNode)){
      edges(keyNode) ++= nodeEdges
    }else{
      edges += (keyNode -> nodeEdges.toBuffer)
    }
    node.children.foreach(breadthFirstTraverse(_, edges))
  }
}

object ColorMapper{
  private val map = Map("scope" -> "blue",
    "entityType" -> "yellow",
    "actionType" -> "orange",
    "filter" -> "red",
    "decision" -> "green").withDefaultValue("blue")
  def color(query:String):String = map(query)
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
