package org.machine.engine.graph

import org.scalatest._
import org.scalatest.mock._

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb._
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.io.fs.FileUtils

//For iterating over Java Collections
import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}

import org.machine.engine.Engine
import org.machine.engine.TestUtils
import org.machine.engine.graph._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._

class LayoutDefinitionSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  private var engine:Engine = null
  private var systemsDataSetId:String = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
  }

  override def afterAll(){
    perge
  }

  it ("should create the property graph with the system layout definitions"){
    val layoutDefs:Seq[LayoutDefinition] = engine.layoutDefinitions()
    import org.machine.engine.viz.GraphVizHelper._
    import org.machine.engine.viz.GraphVizHelper
    visualize(engine.database,
      s"${GraphVizHelper.wd}/viz",
      "temp.dot")
    layoutDefs.length should equal(1)
  }
}
