package org.machine.engine.graph

import org.scalatest._
import org.scalatest.mock._

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.graph._
import org.machine.engine.graph.utilities.DynamicCmdLoader
import org.machine.engine.graph.decisions._
import org.machine.engine.graph.commands._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._

class EngineStatementBuilderSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  var engine:Engine = null
  private var activeUserId:String = null
  override def beforeAll(){
    engine = Engine.getInstance
    perge
    activeUserId = Engine.getInstance
      .createUser
      .withFirstName("Bob")
      .withLastName("Grey")
      .withEmailAddress("onebadclown@derry-maine.com")
      .withUserName("pennywise")
      .withUserPassword("You'll float too...")
    .end
  }

  override def afterAll(){
    perge
  }

  /*
  TODO: Test all 41 options
  */
  describe("Engine Statement Builder"){
    it ("should find all datasets"){
      engine.forUser(activeUserId).createDataSet("Dataset A", "")
      engine.forUser(activeUserId).createDataSet("Dataset B", "")
      engine.forUser(activeUserId).createDataSet("Dataset C", "")
      val result:EngineCmdResult = engine.forUser(activeUserId)
        .setScope(CommandScopes.UserSpaceScope)
        .setActionType(ActionTypes.Retrieve)
        .setEntityType(EntityTypes.DataSet)
        .setFilter(Filters.All)
      .run
      result shouldBe a [QueryCmdResult[_]]
      result.asInstanceOf[QueryCmdResult[DataSet]].results.length should equal(3)
    }

    it("should use reflection"){
      val cmd = DynamicCmdLoader.provision("ListDataSets", null, null, null)
      cmd shouldBe a [ListDataSets]
    }
  }
}
