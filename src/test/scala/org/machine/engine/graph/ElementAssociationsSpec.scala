package org.machine.engine

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

import org.machine.engine.graph._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.logger._

class ElementAssociationsSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbPath = "target/ElementAssociationsSpec.graph"
  val dbFile = new File(dbPath)
  var engine:Engine = null
  var engineOptions = new {
    val logger = new Logger(LoggerLevels.ERROR)
  }
  var notesDataSetId:String = null
  var noteElementDefininitionId:String = null

  override def beforeAll(){
    FileUtils.deleteRecursively(dbFile)
    engine = new Engine(dbPath, engineOptions)
    notesDataSetId = engine.createDataSet("notes", "My collection of notes.")
    noteElementDefininitionId =
      engine
        .onDataSet(notesDataSetId)
        .defineElement("note", "short piece of text")
          .withProperty("title", "String", "The title of the note.")
          .withProperty("description", "String", "An optional description of the note.")
          .withProperty("body", "String", "The body of the note.")
      .end
  }

  override def afterAll(){
    engine.shutdown()
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Machine Engine"){
    describe("DataSet"){
      describe("Element Associations"){
        it("should associate two elements")(pending)
        it("should find outbound associations of an element")(pending)
        it("should find inbound associations of an element")(pending)
        it("should update properties on an association")(pending)
        it("should remove an association between two elements")(pending)
      }
    }
  }
}
