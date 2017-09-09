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


class UserSpaceSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  private var engine:Engine = null
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
            .forUser(activeUserId)
            .inUserSpace
            .defineElement(name, description)
            .withProperty(pname, ptype, pdescription)
            .end

          val useCase = engine.forUser(activeUserId)
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
          engine.forUser(activeUserId)
            .inUserSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine.forUser(activeUserId)
            .inUserSpace()
            .elementDefinitions()
            .find(e => {e.name == "System"})

          val updatedName = "IT System"
          val updatedDescription = """
          |A set of interacting or interdependent components
          | forming an integrated whole.
          """.stripMargin

          engine.forUser(activeUserId)
            .inUserSpace()
            .onElementDefinition(systemOption.get.id)
            .setName(updatedName)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine.forUser(activeUserId)
            .inUserSpace()
            .findElementDefinitionById(systemOption.get.id)

            updatedSystem.name shouldBe updatedName
            updatedSystem.description shouldBe updatedDescription
        }

        it("should update an ElementDefinition's PropertyDefintion"){
          engine.forUser(activeUserId)
            .inUserSpace()
            .defineElement("System", "A thing about a thing...")
            .withProperty("Name", "String", "The name of the system.")
            .end()

          val systemOption = engine.forUser(activeUserId)
            .inUserSpace()
            .elementDefinitions()
            .find(e => {e.name == "System"})

          val updatedName = "System Name"
          val updatedType = "Blob"
          val updatedDescription = "Illogical field. The type doesn't make sense."

          //find the property by its name, then change the name, type and description.
          engine.forUser(activeUserId)
            .inUserSpace()
            .onElementDefinition(systemOption.get.id)
            .editPropertyDefinition("Name")
            .setName(updatedName)
            .setType(updatedType)
            .setDescription(updatedDescription)
            .end()

          val updatedSystem = engine.forUser(activeUserId)
            .inUserSpace()
            .findElementDefinitionById(systemOption.get.id)

          updatedSystem.properties.length shouldBe 1
          updatedSystem.properties(0) should have(
            'name (updatedName),
            'propertyType (updatedType),
            'description (updatedDescription)
          )
        }

        it("should remove an ElementDefinition's PropertyDefintion"){
          engine.forUser(activeUserId)
            .inUserSpace()
            .defineElement("Committee Review", "Critical examination of a document or plan, resulting in a score and/or written feedback.")
            .withProperty("Status", "String", "The current status of the review in the review process.")
            .withProperty("Review Date", "Date", "The date the review will happen or did happen.")
            .withProperty("Rating", "String", "The rating that was issued by the review board.")
            .end()

          val committeReviewOption = engine.forUser(activeUserId)
            .inUserSpace()
            .elementDefinitions()
            .find(e => {e.name == "Committee Review"})

          committeReviewOption.get.properties.length shouldBe 3
          committeReviewOption.get.properties.exists(_.name == "Rating") shouldBe true

          engine.forUser(activeUserId)
            .inUserSpace()
            .onElementDefinition(committeReviewOption.get.id)
            .removePropertyDefinition("Rating")
            .end()

          val committeReview = engine.forUser(activeUserId)
            .inUserSpace()
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

          engine.forUser(activeUserId)
            .inUserSpace
            .defineElement("Architecture Principle", definition)
            .withProperty("Description", "String", "A paragraph about the principle.")
            .withProperty("Heuristic Indicator", "String", "How the principle is measured.")
            .withProperty("Area of Relevence", "String", "Classification of when the principle is appropriate.")
            .end

          val archPrinciple = engine.forUser(activeUserId)
            .inUserSpace
            .findElementDefinitionByName("Architecture Principle")

          engine.forUser(activeUserId)
            .inUserSpace
            .onElementDefinition(archPrinciple.id)
            .delete()
            .end

          val expectedIdMsg = "No element definition with ID: %s could be found in %s".format(archPrinciple.id, "user")
          the [InternalErrorException] thrownBy{
            engine
              .inUserSpace
              .findElementDefinitionById(archPrinciple.id)
          }should have message expectedIdMsg

          the [InternalErrorException] thrownBy{
            engine.forUser(activeUserId)
              .inUserSpace
              .findElementDefinitionByName(archPrinciple.name)
          }should have message Engine.EmptyResultErrorMsg
        }
      }
    }
  }
}
