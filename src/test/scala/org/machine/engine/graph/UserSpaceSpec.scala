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

class UserSpaceSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  val dbPath = "target/UserSpaceSpec.graph"
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
    describe("User Space"){
      it("should have one and only one user space"){
        val findUserSpace = "match (us:internal_user_space) return us.mid as id, us.name as name"
        val userSpaces = query[UserSpace](engine.database, findUserSpace, null, UserSpace.queryMapper)
        userSpaces.isEmpty shouldBe false
        userSpaces.length shouldBe 1
      }

      describe("Element Definitions"){
        it("should create an ElementDefinition"){
          val name = "Use Case"
          val description = "A practical example of a sequence of actions taken to provide value to some user. Often defined as a guide for architecture definition."

          val pname = "Description"
          val ptype = "String"
          val pdescription = "A detailed series of paragraphs capturing the use case."
          engine
            .inUserSpace
            .defineElement(name, description)
            .withProperty(pname, ptype, pdescription)
            .end

          val useCase = engine
            .inUserSpace
            .findElementDefinitionByName("Use Case")

          useCase.name shouldBe name
          useCase.description shouldBe description
          useCase.properties.length shouldBe 1
          useCase.properties(0) should have (
            'name (pname),
            'propertyType (ptype),
            'description (pdescription)
          )
        }

        it("should update both name & defintion"){
          engine
            .inUserSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine
            .inUserSpace()
            .elements()
            .find(e => {e.name == "System"})

          val updatedName = "IT System"
          val updatedDescription = """
          |A set of interacting or interdependent components
          | forming an integrated whole.
          """.stripMargin

          engine
            .inUserSpace()
            .onElementDefinition(systemOption.get.id)
            .setName(updatedName)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine
            .inUserSpace()
            .findElementDefinitionById(systemOption.get.id)

            updatedSystem.name shouldBe updatedName
            updatedSystem.description shouldBe updatedDescription
        }

        it("should update an ElementDefinition's PropertyDefintion"){
          engine
            .inUserSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine
            .inUserSpace()
            .elements()
            .find(e => {e.name == "System"})

          val updatedName = "System Name"
          val updatedType = "Blob"
          val updatedDescription = "Illogical field. The type doesn't make sense."

          //find the property by its name, then change the name, type and description.
          engine
            .inUserSpace()
            .onElementDefinition(systemOption.get.id)
            .editPropertyDefinition("Name")
            .setName(updatedName)
            .setType(updatedType)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine
            .inUserSpace()
            .findElementDefinitionById(systemOption.get.id)

          updatedSystem.properties.length shouldBe 1
          updatedSystem.properties(0) should have(
            'name (updatedName),
            'propertyType (updatedType),
            'description (updatedDescription)
          )
        }

        it("should remove an ElementDefinition's PropertyDefintion")(pending)
        it("should delete an ElementDefinition")(pending)
        it("should list all ElementDefintions")(pending)
        it("should retrieve an ElementDefinition")(pending)
      }
    }
  }
}
