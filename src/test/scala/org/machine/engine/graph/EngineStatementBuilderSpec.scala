package org.machine.engine.graph

import org.scalatest._
import org.scalatest.mock._

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import org.machine.engine.Engine
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._


class EngineStatementBuilderSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbFile = new File(Engine.databasePath)
  var engine:Engine = null
  override def beforeAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
  }

  override def afterAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  /*
  TODO: Test all 41 options
  Consider invocation of method using reflection vs cmd pattern.
  Engine.getInstance.datasets()
  */
  describe("Engine Statement Builder"){
    /*
    engine
      .inUserSpace(user)
      .retrieve
      .
    */
    ignore ("should find all datasets"){
      val request = new Request(Some("da user"),
        Some("retrieve"),
        CommandScopes.UserSpaceScope,
        Some("DataSet"),
        Some("All"))

      val result =
      engine
        .reset
        .setUser(request.user)
        .setScope(request.scope)
        .setActionType(request.actionType)
        .setEntityType(request.entityType)
        .setFilter(request.filter)
      .run //finds Cmd and executes it.

      Console.println(result)
    }

    it ("should find ListDataSets"){
      val request = new Request(Some("da user"),
        Some("retrieve"),
        CommandScopes.UserSpaceScope,
        Some("DataSet"),
        Some("All"))

      val decisionTree = buildDecisionTree()
      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("ListDataSets")
    }

    it ("should find FindDataSetById"){
      val request = new Request(Some("da user"),
        Some("retrieve"),
        CommandScopes.UserSpaceScope,
        Some("DataSet"),
        Some("ID"))

      val decisionTree = buildDecisionTree()
      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("FindDataSetById")
    }

    it ("should find FindDataSetByName"){
      val request = new Request(Some("da user"),
        Some("retrieve"),
        CommandScopes.UserSpaceScope,
        Some("DataSet"),
        Some("Name"))

      val decisionTree = buildDecisionTree()
      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("FindDataSetByName")
    }
  }

  /*
  I need to traverse the tree and at each question node, find the match for the question attribute.
  */
  def buildDecisionTree1():Question = {
    //scope? ~> entity? ~> action? ~> filter?
    val root = Question("scope")
    root ~> Opt(CommandScopes.UserSpaceScope.scope) ~> Question("entityType") ~> Opt("DataSet") ~> Question("actionType") ~> Opt("retrieve") ~> Question("filter") ~> Opt("All") -> Decision("ListDataSets")
    return root
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

//Eventually use org.machine.engine.flow.requests.RequestMessage
case class Request(user: Option[String],
  actionType: Option[String], //create, retrieve, update, delete
  scope: CommandScope, // system, user, dataset
  entityType: Option[String], //ElementDefinition, DataSet, Element, Association, None
  filter: Option[String]//None, ID, Name
){
  def toMap():Map[String, Option[String]] = {
    return Map("user" -> user,
      "actionType" -> actionType,
      "scope" -> Some(scope.scope),
      "entityType" -> entityType,
      "filter" -> filter)
  }
}

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
}

//Questions can only have options.
//Options have a single question or decisions.
import scala.collection.mutable
class Question(at: String){
  val attribute = at
  private val options = mutable.Map[String, Opt]()

  def ~>(option: Opt):Opt = {
    options += (option.name -> option)
    return option
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
}

object Question{
  def apply(attribute: String):Question = {
    return new Question(attribute)
  }
}

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

case class Decision(val name: String)
