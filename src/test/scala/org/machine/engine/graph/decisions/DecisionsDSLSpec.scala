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
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.All)
      val decision = DecisionDSL.findDecision(decisionTree, request)
      decision.name should equal("ListDataSets")
    }

    it ("should find FindDataSetById"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.ID)
      val decision = DecisionDSL.findDecision(decisionTree, request)
      decision.name should equal("FindDataSetById")
    }

    it ("should find FindDataSetByName"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.Name)

      val decision = DecisionDSL.findDecision(decisionTree, request)
      decision.name should equal("FindDataSetByName")
    }

    ignore("should draw the tree"){
      DecisionDSL.drawTree(decisionTree,0, new ConsolePlotter())
    }

    it("should build the tree from the rules"){
      val url = getClass.getResource("/org/machine/engine/graph/decisions/rules")
      val path = url.getPath()
      val tree = DecisionDSL.buildDecisionTreeFromRules(path)
      val diagram = DecisionDSL.createDotFile(tree)
      val expected = """
      |digraph EngineDecisionTree{
      |  UserSpace->{entityType}
      |  filter->{All ID Name}
      |  All->{ListDataSets}
      |  Retrieve->{filter}
      |  ID->{FindDataSetById}
      |  DataType->{actionType}
      |  actionType->{Retrieve}
      |  scope->{UserSpace}
      |  entityType->{DataType}
      |  Name->{FindDataSetByName}
      |}
      """.stripMargin.replaceAll(" ", "")
      diagram.replaceAll(" ", "").replaceAll("\t","") should equal(expected)
    }
  }
}
