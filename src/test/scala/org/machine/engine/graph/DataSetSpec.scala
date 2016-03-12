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

class DataSetSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbPath = "target/DataSetSpec.graph"
  val dbFile = new File(dbPath)
  var engine:Engine = null
  var engineOptions = new {
    val logger = new Logger(LoggerLevels.ERROR)
  }

  override def beforeAll(){
    FileUtils.deleteRecursively(dbFile)
    engine = new Engine(dbPath, engineOptions)
  }

  override def afterAll(){
    engine.shutdown()
    FileUtils.deleteRecursively(dbFile)
  }

  def elementDefAndPropDefQueryMapper( results: ArrayBuffer[(ElementDefinition, PropertyDefinition)],
    record: java.util.Map[java.lang.String, Object]) = {
    val elementId = record.get("elementId").toString()
    val elementName = record.get("elementName").toString()
    val elementDescription = record.get("elementDescription").toString()
    val ed = new ElementDefinition(elementId, elementName, elementDescription)

    val propId = record.get("propId").toString()
    val propName = record.get("propName").toString()
    val propType = record.get("propType").toString()
    val propDescription = record.get("propDescription").toString()
    val pd = new PropertyDefinition(propId, propName, propType, propDescription)
    val pair = (ed, pd)
    results += pair
  }

  def consolidateElementDefs(records: List[(ElementDefinition, PropertyDefinition)]):List[ElementDefinition] ={
    val elementsMap = Map[String, ElementDefinition]()
    var ed:ElementDefinition = null;
    var pd:PropertyDefinition = null;
    records.foreach(r => {
      ed = r._1
      pd = r._2
      if(elementsMap.contains(ed.id)){
        elementsMap.get(ed.id).get.addProperty(pd)
      }else{
        ed.addProperty(pd)
        elementsMap += (ed.id -> ed)
      }
    })
    return elementsMap.values.toList
  }

  describe("Machine Engine"){
    describe("DataSet"){
      it("should create a DataSet"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.createDataSet(datasetName, datasetDescription)
        val dsOption = engine.datasets().find(ds => {ds.name == datasetName})
        dsOption.get should have(
          'name (datasetName),
          'description (datasetDescription)
        )
      }

      it("should retrieve a DataSet by name"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.createDataSet(datasetName, datasetDescription)
        val ds = engine.findDataSetByName(datasetName)
        ds should have(
          'name (datasetName),
          'description (datasetDescription)
        )
      }

      it("should update both name & defintion"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.createDataSet(datasetName, datasetDescription)
        val ds = engine.findDataSetByName(datasetName)

        val newName = "Capability Definitions: V1"
        val newDescription = "Version 1 of business capabilities definitions."
        engine
          .onDataSet(ds.id)
          .setName(newName)
          .setDescription(newDescription)
          .end

        val modifiedDS = engine.findDataSetById(ds.id)

        modifiedDS should have(
          'id (ds.id),
          'name (newName),
          'description (newDescription),
          'creationTime (ds.creationTime)
        )

        modifiedDS.lastModifiedType.length should be > 0
      }

      it("should delete a DataSet")(pending)

      it("should list all available DataSets"){
        engine
          .createDataSet("A", "Empty Data Set")
          .createDataSet("B", "Empty Data Set")
          .createDataSet("C", "Empty Data Set")
          .createDataSet("D", "Empty Data Set")

        engine.datasets().length should be >= 4
      }

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

          engine
            .createDataSet(datasetName, "A collection of business capabilities.")
            .onDataSetByName(datasetName)
            .defineElement(bizCapName, bizCapDef)
              .withProperty("name", "String", "The name of the business capability.")
              .withProperty("description", "String", "A short paragraph describing the business capability.")
            .end

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
            .onDataSetByName(datasetName)
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

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
            .end()

          val updatedSystem = engine
            .onDataSetByName(datasetName)
            .findElementDefinitionById(system.id)

          updatedSystem.name shouldBe updatedName
          updatedSystem.description shouldBe updatedDescription
        }

        it("should update an ElementDefinition's PropertyDefintion"){
          val datasetName = "Architecture Components"
          engine
            .createDataSet(datasetName, "A collection of system architecture components.")
            .onDataSetByName(datasetName)
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

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
            .end()

          val updatedSystem = engine
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
          engine
            .createDataSet(datasetName, "A collection of system architecture components.")
            .onDataSetByName(datasetName)
            .defineElement(edName, "Critical examination of a document or plan, resulting in a score and/or written feedback.")
            .withProperty("Status", "String", "The current status of the review in the review process.")
            .withProperty("Review Date", "Date", "The date the review will happen or did happen.")
            .withProperty("Rating", "String", "The rating that was issued by the review board.")
            .end()

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
            .end()

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
            .delete()
            .end

          val expectedIdMsg = "No element with ID: %s could be found in dataset: %s".format(archPrinciple.id, datasetName)
          the [InternalErrorException] thrownBy{
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionById(archPrinciple.id)
          }should have message expectedIdMsg

          val expectedNameMsg = "No element with Name: %s could be found in dataset: %s".format(archPrinciple.name, datasetName)
          the [InternalErrorException] thrownBy{
            engine
              .onDataSetByName(datasetName)
              .findElementDefinitionByName(archPrinciple.name)
          }should have message expectedNameMsg
        }

        it("should list all ElementDefintions"){
          engine
            .createDataSet("X", "A mysterious dataset.")
            .onDataSetByName("X")
            .defineElement("AA", "blah")
            .withProperty("AA:P", "String", "Blah blah")
          .end
            .onDataSetByName("X")
            .defineElement("BB", "blah")
            .withProperty("BB:P", "String", "Blah blah")
          .end
            .onDataSetByName("X")
            .defineElement("CC", "blah")
            .withProperty("CC:P", "String", "Blah blah")
          .end

          val elementDefs =
            engine
              .onDataSetByName("X")
              .elements

          elementDefs should have length 3
        }
      }

      describe("Element Instances"){
        it("should create an Element")(pending)
        it("should retrieve an Element")(pending)
        it("should update an Element")(pending)
        it("should delete an Element")(pending)
        it("should find a specific Element by ID")(pending)
        it("should associate two elements")(pending)
        it("should find outbound associations of an element")(pending)
        it("should find inbound associations of an element")(pending)
        it("should update properties on an association")(pending)
        it("should remove an association between two elements")(pending)
      }
    }
  }
}
