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

import org.machine.engine._
import org.machine.engine.graph._
import org.machine.engine.exceptions._
import org.machine.engine.graph.nodes._
import org.machine.engine.logger._

class SystemSpaceSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbPath = "target/SystemSpaceSpec.graph"
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
    describe("System Space"){
      it("should have one and only one system space"){
        val findSystemSpace = "match (ss:internal_system_space) return ss.mid as id, ss.name as name"
        val systemSpaces = query[SystemSpace](engine.database, findSystemSpace, null, SystemSpace.queryMapper)
        systemSpaces.isEmpty shouldBe false
        systemSpaces.length shouldBe 1
      }

      describe("Element Definitions"){
        it("should create an ElementDefinition"){
          engine
            .inSystemSpace()
            .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
            .withProperty("Note Text", "String", "The body of the note.")
            .withProperty("Title", "String", "A user defined title of the note.")
            .end()

            val findDefinedElements = """
              |match (ss:internal_system_space)-[:exists_in]->(ed:element_definition)-[:composed_of]->(pd:property_definition)
              |return ed.mid as elementId,
              |  ed.name as elementName,
              |  ed.description as elementDescription,
              |  pd.mid as propId,
              |  pd.name as propName,
              |  pd.type as propType,
              |  pd.description as propDescription
              """.stripMargin

            val records = query[(ElementDefinition, PropertyDefinition)](engine.database,
              findDefinedElements, null,
              elementDefAndPropDefQueryMapper)

            records.length shouldBe 2
            val elements:List[ElementDefinition] = consolidateElementDefs(records.toList)
            elements.length shouldBe 1
            elements(0).properties.length shouldBe 2
        }

        it("should retrieve all ElementDefinitions"){
          engine
            .inSystemSpace()
            .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
            .withProperty("Note Text", "String", "The body of the note.")
            .withProperty("Title", "String", "A user defined title of the note.")
            .end()

          val elements:List[ElementDefinition] = engine
            .inSystemSpace()
            .elementDefinitions()

          elements.length > 1
        }

        it("should retrieve a specific ElementDefinition by ID"){
          engine
            .inSystemSpace()
            .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
            .withProperty("Note Text", "String", "The body of the note.")
            .withProperty("Title", "String", "A user defined title of the note.")
            .end()

            val elements:List[ElementDefinition] = engine
              .inSystemSpace()
              .elementDefinitions()

            val noteOption = elements.find(e => {e.name == "Note"})
            val noteElement = engine
              .inSystemSpace()
              .findElementDefinitionById(noteOption.get.id)
            noteElement.id shouldBe noteOption.get.id
            noteElement.name shouldBe noteOption.get.name
        }

        it("should update both name & defintion"){
          engine
            .inSystemSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine
            .inSystemSpace()
            .elementDefinitions()
            .find(e => {e.name == "System"})

          val updatedName = "IT System"
          val updatedDescription = """
          |A set of interacting or interdependent components
          | forming an integrated whole.
          """.stripMargin

          engine
            .inSystemSpace()
            .onElementDefinition(systemOption.get.id)
            .setName(updatedName)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine
            .inSystemSpace()
            .findElementDefinitionById(systemOption.get.id)

            updatedSystem.name shouldBe updatedName
            updatedSystem.description shouldBe updatedDescription
        }

       it("should update an ElementDefinition's PropertyDefintion"){
          engine
            .inSystemSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine
            .inSystemSpace()
            .elementDefinitions()
            .find(e => {e.name == "System"})

          val updatedName = "System Name"
          val updatedType = "Blob"
          val updatedDescription = "Illogical field. The type doesn't make sense."

          //find the property by its name, then change the name, type and description.
          engine
            .inSystemSpace()
            .onElementDefinition(systemOption.get.id)
            .editPropertyDefinition("Name")
            .setName(updatedName)
            .setType(updatedType)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine
            .inSystemSpace()
            .findElementDefinitionById(systemOption.get.id)

          updatedSystem.properties.length shouldBe 1
          updatedSystem.properties(0).name shouldBe updatedName
          updatedSystem.properties(0).propertyType shouldBe updatedType
          updatedSystem.properties(0).description shouldBe updatedDescription
        }

        it("should remove an ElementDefinition's PropertyDefintion"){
          engine
            .inSystemSpace()
            .defineElement("Committee Review", "Critical examination of a document or plan, resulting in a score and/or written feedback.")
            .withProperty("Status", "String", "The current status of the review in the review process.")
            .withProperty("Review Date", "Date", "The date the review will happen or did happen.")
            .withProperty("Rating", "String", "The rating that was issued by the review board.")
            .end()

          val committeReviewOption = engine
            .inSystemSpace()
            .elementDefinitions()
            .find(e => {e.name == "Committee Review"})

          committeReviewOption.get.properties.length shouldBe 3
          committeReviewOption.get.properties.exists(_.name == "Rating") shouldBe true

          engine
            .inSystemSpace()
            .onElementDefinition(committeReviewOption.get.id)
            .removePropertyDefinition("Rating")
            .end()

          val committeReview = engine
            .inSystemSpace()
            .findElementDefinitionById(committeReviewOption.get.id)

          committeReview.properties.length shouldBe 2
          committeReview.properties.exists(_.name == "Rating") shouldBe false
        }

        it("should delete an ElementDefinition"){
          val definition = """
          |A fundamental statement of belief, approach, or intent that guides
          |the definition of an architecture. It may refer to current circumstances
          |or to a desired future state. A good principle is constructive, reasoned,
          |well articulated, testable, and significant.
          """.stripMargin

          engine
            .inSystemSpace
            .defineElement("Architecture Principle", definition)
            .withProperty("Description", "String", "A paragraph about the principle.")
            .withProperty("Heuristic Indicator", "String", "How the principle is measured.")
            .withProperty("Area of Relevence", "String", "Classification of when the principle is appropriate.")
            .end

          val archPrinciple = engine
            .inSystemSpace
            .findElementDefinitionByName("Architecture Principle")

          engine
            .inSystemSpace
            .onElementDefinition(archPrinciple.id)
            .delete()
          .end

          val expectedIdMsg = "No element with ID: %s could be found in %s".format(archPrinciple.id, "internal_system_space")
          the [InternalErrorException] thrownBy{
            engine
              .inSystemSpace
              .findElementDefinitionById(archPrinciple.id)
          }should have message expectedIdMsg

          val expectedNameMsg = "No element with Name: %s could be found in %s".format(archPrinciple.name, "internal_system_space")
          the [InternalErrorException] thrownBy{
            engine
              .inSystemSpace
              .findElementDefinitionByName(archPrinciple.name)
          }should have message expectedNameMsg
        } 
      }
    }
  }
}
