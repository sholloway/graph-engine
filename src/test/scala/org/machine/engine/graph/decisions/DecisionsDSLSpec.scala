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

  val url = getClass.getResource("/org/machine/engine/graph/decisions/rules")
  val path = url.getPath()
  var tree:Question = null

  override def beforeAll(){
    tree = DecisionDSL.buildDecisionTreeFromRules(path)
  }

  describe("Decision Tree"){
    it ("should find ListDataSets"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.All)
      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("ListDataSets")
    }

    it ("should find FindDataSetById"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.ID)
      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("FindDataSetById")
    }

    it ("should find FindDataSetByName"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.UserSpaceScope,
        EntityTypes.DataSet,
        Filters.Name)

      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("FindDataSetByName")
    }

    it ("should find ListAllElementDefinitions"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Retrieve,
        CommandScopes.SystemSpaceScope,
        EntityTypes.ElementDefinition,
        Filters.All)

      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("ListAllElementDefinitions")
    }

    it ("should find CreateElementDefinition"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Create,
        CommandScopes.SystemSpaceScope,
        EntityTypes.ElementDefinition,
        Filters.None)

      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("CreateElementDefintion")
    }

    it ("should find UpdateElementDefinition"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Update,
        CommandScopes.SystemSpaceScope,
        EntityTypes.ElementDefinition,
        Filters.None)

      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("EditElementDefintion")
    }

    it ("should find DeleteElementDefinition"){
      val request = DecisionRequest(Some("da user"),
        ActionTypes.Delete,
        CommandScopes.SystemSpaceScope,
        EntityTypes.ElementDefinition,
        Filters.None)

      val decision = DecisionDSL.findDecision(tree, request)
      decision.name should equal("DeleteElementDefintion")
    }

    /* FIXME Replace this.
    This is a a worthless test.
    */
    it("should draw the tree"){
      DecisionDSL.drawTree(tree,0, new ConsolePlotter())
    }

    /* FIXME Replace this.
    This is a a worthless test.
    */
    it("should build the tree from the rules"){
      val diagram = DecisionDSL.createDotFile(tree)
      Console.println(diagram)
      val expected = """
      |digraph EngineDecisionTree{
      |	filter->{All ID Name}
      |	All->{ListDataSets}
      |	Retrieve->{filter}
      |	internal_user_space->{entityType}
      |	ID->{FindDataSetById}
      |	actionType->{Retrieve}
      |	scope->{internal_user_space}
      |	entityType->{DataSet}
      |	Name->{FindDataSetByName}
      |	DataSet->{actionType}
      |}
      """.stripMargin.replaceAll(" ", "").replaceAll("\t","")
      // diagram.replaceAll(" ", "").replaceAll("\t","") should equal(expected)
    }
  }
}
