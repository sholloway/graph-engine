package org.machine.engine.graph.decisions

import org.scalatest._
import org.scalatest.mock._

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import org.machine.engine.Engine
import org.machine.engine.graph._
import org.machine.engine.graph.decisions._
import org.machine.engine.graph.commands._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._


class DecisionsDSLSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbFile = new File(Engine.databasePath)
  var engine:Engine = null

  val decisionTree = DecisionDSL.buildDecisionTree()

  override def beforeAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
    engine = Engine.getInstance
  }

  override def afterAll(){
    Engine.shutdown
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Decision Tree"){
    it ("should find ListDataSets"){
      val request = new Request(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.All)
      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("ListDataSets")
    }

    it ("should find FindDataSetById"){
      val request = new Request(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.ID)
      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("FindDataSetById")
    }

    it ("should find FindDataSetByName"){
      val request = new Request(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.Name)

      val decision = DecisionDSL.findDecision(decisionTree, request.toMap)
      decision.name should equal("FindDataSetByName")
    }
  }

  case class Request(user: Option[String],
    actionType: ActionType,
    scope: CommandScope, // system, user, dataset
    entityType: EntityType, //ElementDefinition, DataSet, Element, Association, None
    filter: Filter//None, ID, Name
  ){
    def toMap():Map[String, Option[String]] = {
      return Map("user" -> user,
        "actionType" -> Some(actionType.value),
        "scope" -> Some(scope.scope),
        "entityType" -> Some(entityType.value),
        "filter" -> Some(filter.value))
    }
  }  
}
