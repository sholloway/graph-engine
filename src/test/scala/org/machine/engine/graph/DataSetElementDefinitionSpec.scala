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

class DataSetElementDefinitionSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  var engine:Engine = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
  }

  override def afterAll(){
    perge
  }

  describe("Machine Engine"){
    describe("DataSet"){
      describe("Element Definitions"){
        it("should create an ElementDefinition"){
          val datasetName = "Capability Definitions"

          val bizCapName = "Business Capability"
          val bizCapDef = """
          The expression or the articulation of the capacity, materials and
          expertise an organization needs in order to perform core functions.
          """

          engine
            .createDataSet(datasetName, "A collection of business capabilities.")

          engine
            .onDataSetByName(datasetName)
            .defineElement(bizCapName, bizCapDef)
              .withProperty("name", "String", "The name of the business capability.")
              .withProperty("description", "String", "A short paragraph describing the business capability.")
            .end

          val bizCap =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName(bizCapName)

          bizCap should have(
            'name (bizCapName),
            'description (bizCapDef)
          )

          bizCap.properties should have length 2
        }

        it("should retrieve an ElementDefinition by ID"){
          val datasetName = "Capability Definitions"
          val bizCapName = "Business Capability"
          val bizCapDef = """
          The expression or the articulation of the capacity, materials and
          expertise an organization needs in order to perform core functions.
          """

          val bizCapByName =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName(bizCapName)

          val bizCapById =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(bizCapByName.id)

          bizCapById should have(
            'name (bizCapName),
            'description (bizCapDef)
          )

          bizCapById.properties should have length 2
        }

        it("should update both name & defintion"){
          val datasetName = "Architecture Components"
          engine
            .createDataSet(datasetName, "A collection of system architecture components.")

          engine
            .onDataSetByName(datasetName)
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
          .end

          val system =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName("System")

          val updatedName = "IT System"
          val updatedDescription = """
          |A set of interacting or interdependent components
          | forming an integrated whole.
          """.stripMargin

          engine
            .onDataSetByName(datasetName)
            .onElementDefinition(system.id)
            .setName(updatedName)
            .setDescription(updatedDescription)
          .end

          val updatedSystem =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(system.id)

          updatedSystem.name shouldBe updatedName
          updatedSystem.description shouldBe updatedDescription
        }

        it("should update an ElementDefinition's PropertyDefintion"){
          val datasetName = "Architecture Components"
          engine
            .createDataSet(datasetName, "A collection of system architecture components.")

          engine
            .onDataSetByName(datasetName)
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
          .end

          val system =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName("System")

          val updatedName = "System Name"
          val updatedType = "Blob"
          val updatedDescription = "Illogical field. The type doesn't make sense."

          //find the property by its name, then change the name, type and description.
          engine
            .onDataSetByName(datasetName)
            .onElementDefinition(system.id)
            .editPropertyDefinition("Name")
            .setName(updatedName)
            .setType(updatedType)
            .setDescription(updatedDescription)
          .end

          val updatedSystem =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(system.id)

          updatedSystem.properties should have length 1
          updatedSystem.properties(0) should have(
            'name (updatedName),
            'propertyType (updatedType),
            'description (updatedDescription)
          )
        }

        it("should remove an ElementDefinition's PropertyDefintion"){
          val datasetName = "Architecture Components"
          val edName = "Committee Review"
          engine.createDataSet(datasetName, "A collection of system architecture components.")
          engine
            .onDataSetByName(datasetName)
            .defineElement(edName, "Critical examination of a document or plan, resulting in a score and/or written feedback.")
            .withProperty("Status", "String", "The current status of the review in the review process.")
            .withProperty("Review Date", "Date", "The date the review will happen or did happen.")
            .withProperty("Rating", "String", "The rating that was issued by the review board.")
          .end

          val committeReview =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName(edName)

          committeReview.properties should have length 3
          committeReview.properties.exists(_.name == "Rating") shouldBe true

          engine
            .onDataSetByName(datasetName)
            .onElementDefinition(committeReview.id)
            .removePropertyDefinition("Rating")
          .end

          val updatedCommitteReview =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(committeReview.id)

          updatedCommitteReview.properties should have length 2
          updatedCommitteReview.properties.exists(_.name == "Rating") shouldBe false
        }

        it("should delete an ElementDefinition"){
          val datasetName = "Architecture Components"

          val definition = """
          |A fundamental statement of belief, approach, or intent that guides
          |the definition of an architecture. It may refer to current circumstances
          |or to a desired future state. A good principle is constructive, reasoned,
          |well articulated, testable, and significant.
          """.stripMargin

          engine
            .createDataSet(datasetName, "A collection of system architecture components.")

          engine
            .onDataSetByName(datasetName)
            .defineElement("Architecture Principle", definition)
            .withProperty("Description", "String", "A paragraph about the principle.")
            .withProperty("Heuristic Indicator", "String", "How the principle is measured.")
            .withProperty("Area of Relevence", "String", "Classification of when the principle is appropriate.")
            .end

          val archPrinciple =
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName("Architecture Principle")

          engine
            .onDataSetByName(datasetName)
            .onElementDefinition(archPrinciple.id)
            .delete
          .end

          val expectedIdMsg = "No element definition with ID: %s could be found in dataset: %s".format(archPrinciple.id, datasetName)
          the [InternalErrorException] thrownBy{
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(archPrinciple.id)

          }should have message expectedIdMsg

          the [InternalErrorException] thrownBy{
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName(archPrinciple.name)
          }should have message Engine.EmptyResultErrorMsg
        }

        it("should list all ElementDefintions"){
          engine
            .createDataSet("X", "A mysterious dataset.")

          engine
            .onDataSetByName("X")
            .defineElement("AA", "blah")
            .withProperty("AA:P", "String", "Blah blah")
          .end

          engine
            .onDataSetByName("X")
            .defineElement("BB", "blah")
            .withProperty("BB:P", "String", "Blah blah")
          .end

          engine
            .onDataSetByName("X")
            .defineElement("CC", "blah")
            .withProperty("CC:P", "String", "Blah blah")
          .end

          val elementDefs =
            engine
              .onDataSetByName("X")
              .elementDefinitions

          elementDefs should have length 3
        }

      }
    }
  }
}
