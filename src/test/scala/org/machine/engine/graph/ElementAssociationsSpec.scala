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
  var systemsDataSetId:String = null
  var noteElementDefininitionId:String = null
  var systemElementDefinitionId:String = null
  var systemId:String = null
  var noteId:String = null

  override def beforeAll(){
    FileUtils.deleteRecursively(dbFile)
    engine = new Engine(dbPath, engineOptions)
    systemsDataSetId = engine.createDataSet("System Under Review", "System that need to be reviewed.")
    noteElementDefininitionId =
      engine
        .onDataSet(systemsDataSetId)
        .defineElement("note", "short piece of text")
          .withProperty("title", "String", "The title of the note.")
          .withProperty("description", "String", "An optional description of the note.")
          .withProperty("body", "String", "The body of the note.")
      .end

    systemElementDefinitionId =
      engine
        .onDataSet(systemsDataSetId)
        .defineElement("system", "A set of interacting or interdependent components forming an integrated whole.")
          .withProperty("name", "String", "The name of the system.")
          .withProperty("description", "String", "An optional description of the system.")
      .end

    systemId = engine
      .onDataSet(systemsDataSetId)
      .provision(systemElementDefinitionId)
        .withField("name", "Publishing System")
        .withField("description", "Publishes stuff.")
    .end

    noteId = engine
      .onDataSet(systemsDataSetId)
      .provision(noteElementDefininitionId)
        .withField("title", "Quick Note")
        .withField("body", "Talk to Chuck about what is being published.")
    .end
  }

  override def afterAll(){
    engine.shutdown()
    FileUtils.deleteRecursively(dbFile)
  }

  describe("Machine Engine"){
    describe("DataSet"){
      describe("Element Associations"){
        it("should associate two elements"){
          val annotationId = engine
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "A. Sterling")
          .end

          val annotation = engine
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          annotation should have(
            'id (annotationId),
            'associationType ("annotates")
          )

          annotation.fields should have size 1
          annotation.field[String]("createdBy") should equal("A. Sterling")
        }

        it ("should associate without specifying as()"){
          val annotationId = engine
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .withField("createdBy", "A. Sterling")
          .end

          val annotation = engine
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          annotation should have(
            'id (annotationId),
            'associationType ("is_associated_with")
          )

          annotation.fields should have size 1
          annotation.field[String]("createdBy") should equal("A. Sterling")
        }

        it("should update properties on an association"){        
          val annotationId = engine
            .onDataSet(systemsDataSetId)
            .attach(noteId)
            .to(systemId)
            .as("annotates")
            .withField("createdBy", "A. Sterling")
          .end

          engine
            .onDataSet(systemsDataSetId)
            .onAssociation(annotationId)
            .setField("createdBy", "Pam")
          .end

          val association = engine
            .onDataSet(systemsDataSetId)
            .findAssociation(annotationId)

          association.field[String]("createdBy") should equal("Pam")
        }

        it("should remove an association between two elements")(pending)

        //Should this find nodes or relationships?
        //Probably need both.
        it("should find outbound associations of an element")(pending)
        it("should find inbound associations of an element")(pending)

      }
    }
  }
}
