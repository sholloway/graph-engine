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
import org.machine.engine.graph.utilities.DynamicCmdLoader
import org.machine.engine.graph.decisions._
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
  */
  describe("Engine Statement Builder"){
    it ("should find all datasets"){
      engine.createDataSet("Dataset A", "")
      engine.createDataSet("Dataset B", "")
      engine.createDataSet("Dataset C", "")
      val result:EngineCmdResult = engine
        .reset
        .setUser(Some("da user"))
        .setScope(CommandScopes.UserSpaceScope)
        .setActionType(ActionTypes.Retrieve)
        .setEntityType(EntityTypes.DataSet)
        .setFilter(Filters.All)
      .run
      result shouldBe a [QueryCmdResult[_]]
      result.asInstanceOf[QueryCmdResult[DataSet]].results.length should equal(3)
    }

    it("should use refelction"){
      val cmd = DynamicCmdLoader.provision("ListDataSets", null, null, null)
      cmd shouldBe a [ListDataSets]
    }
  }
}
