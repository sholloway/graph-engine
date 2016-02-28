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
          engine
            .inUserSpace()
            .defineElement("Use Case", "A practical example of a sequence of actions taken to provide value to some user. Often defined as a guide for architecture definition.")
            .withProperty("Description", "String", "A detailed series of paragraphs capturing the use case.")
            .end()

          val findDefinedElements = """
            |match (ss:internal_user_space)-[:exists_in]->(ed:element_definition)-[:composed_of]->(pd:property_definition)
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

          records.length shouldBe 1
          val elements:List[ElementDefinition] = consolidateElementDefs(records.toList)
          elements.length shouldBe 1
          elements(0).properties.length shouldBe 1
        }

        it("should retrieve an ElementDefinition")(pending)
        it("should update an ElementDefinition")(pending)
        it("should delete an ElementDefinition")(pending)
        it("should list all ElementDefintions")(pending)
      }
    }
  }
}
