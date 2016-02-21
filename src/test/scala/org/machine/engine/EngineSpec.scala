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
import org.machine.engine.graph.nodes._
import org.machine.engine.logger._

class EngineSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  /*
  The Challenge I see with ElementDefinitions is contracts are going to evolve
  towards Maps.

  Thoughts:
    - setProperty could look up the definition of an element and if the
      property name exists, it could attempt to enforce the type.
    - setProperty should compare property names in lower case.
    - Property names will be stored in all lower case with white space replaced with underscores.
    - Have all provisioned elements contain creation_time, last_modified_time, uuid (Do not expose Neo4J ID)
      Go ahead and have an UUID so, if I ever do make this p2p or client/server,
      the IDs are ready.

    - ElementDefintetions can be tied to SystemSpace, UserSpace, DataSet
      SystemSpace - Can be instantiated, but not redefined by the User.
      UserSpace - Defined by the user, can be instantiated in any DataSet.
      DataSet - Defined by the user, is not visible outside of the related DataSet.
    - On the fluent API, .end() is when the cypher statement is finally constructed and executed.

    -Does a Fluent interface makesense? The engine will be accessed by a CQRS command over 0MQ.
     The engine code will need to take the command and convert it into the appropriate cypher queries.


     Provision a ElementDefinition in System Space.
     engine
       .inSystemSpace()
       .defineElement("Note", "A brief record of something, captured to assist the memory or for future reference.")
       .addProperty("Note Text", String, "The body of the note.")
       .addProperty("Title", String, "A user defined title of the note.")
       .end()

     Provision an element in UserSpace.
     engine
       .inUserSpace()
       .defineElement("...")
       .addProperty("..")
       .end()

     Create a DataSet in the UserSpace.
     engine
       .createDataSet("Capability Definitions", "A list of business capabilities at my company.")
       .end()

     In the UserSpace, define an element in a specific DataSet.
     engine
       .inDataSet("Capability Definitions")
       .defineElement("Business Capability", "...")
       .addProperty("...", "...")
       .end()

     In a DataSet, create an instance of a predefined element.
     engine
       .inDataSet("Capability Definitions")
       .provisionElement("Business Capability")
       .withProperties(Map("name"->"...", "description"->"..."))
       .withProperty("..","..")
       .end()

     List all defined elements in SystemSpace.
     engine
       .inSystemSpace()
       .elements()

     List all defined elements in UserSpace.
     engine
       .inUserSpace()
       .elements()

     List all defined elements in a DataSet.
     engine
       .inDataSet("..")
       .elements()

     List all provisioned elements in a DataSet
     I will need to filter elements. Possible chain filters?
     Do you filter them in cypher or as array objects?
     The below would load all nodes into memory, then reduce the returned result set.
     engine
       .inDataSet("..")
       .listElements()
       .filter(filter-function)
       .end()

     Find elements by appending a cypher clause to the primary cypher clause.
     I'm always going to need to start a query with the graph starting from a DataSet.
     engine
       .inDataSet("..")
       .query(cypher)

     How does an update work? The client changes a Node property.
     Will need to select a node...
     engine
       .inDataSet("..")
       .findUnique(Map()) || .findById(id)
       .update(Map())
       .end()

     Make an association
     The below would be awkward to implement.
       .withProperty would need to return a Relationship to chain properties
       but to find the next node, it would need to return the DataSet
     engine
       .inDataSet("..")
       .findById(id)
       .associate("relationship name")
         .withProperty("..","..")
         .withProperty("..","..")
         .withProperties(Map())
       .findById(id)
       end()
  */


  val dbPath = "target/EngineSpec.graph"
  val dbFile = new File(dbPath)
  var engine:Engine = null
  var engineOptions = new {
    val logger = new Logger(LoggerLevels.DEBUG)
  }

  override def beforeAll(){
    FileUtils.deleteRecursively(dbFile)
    engine = new Engine(dbPath, engineOptions)
  }

  override def afterAll(){
    engine.shutdown()
    FileUtils.deleteRecursively(dbFile)
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
        /*
        Need to:
          Create Nodes
            ElementDefinition
              mid: uuid
              name: Note
              description: String
              creation_time:Timestamp
              last_modified_time:Timestamp

            PropertyDefinition
              mid: uuid
              name: Note Text
              type: String
              creation_time:Timestamp
              last_modified_time:Timestamp

            PropertyDefinition
              mid: uuid
              name: Title
              type: String
              creation_time:Timestamp
              last_modified_time:Timestamp

          Create Associations
            note -[:composed_of]-> Note Text
            note -[:composed_of]-> Title

        Statements
        merge(ed:element_definition
          {mid:{uuid},
          name:{name},
          description:{description}
        })
        on create set ed.creation_time = timestamp()
        on match set ed.last_modified_time = timestamp()

        merge(pd:property_definition {name:"a", type:"String"}) return pd
        merge(pd:property_definition {name:"a", type:"String"}) return pd

        match (ed:element_definition) where ed.name="note"
        match (pd:property_definition)
        merge (ed)-[:composed_of]->(pd)
        */
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
              |  pd.mid as propId,
              |  pd.name as propName,
              |  pd.type as propType,
              |  pd.description as propDescription
              """.stripMargin

            val records = query[(ElementDefinition, PropertyDefinition)](engine.database,
              findDefinedElements, null,
              ( results: ArrayBuffer[(ElementDefinition, PropertyDefinition)],
                record: java.util.Map[java.lang.String, Object]) => {
                val elementId = record.get("elementId").toString()
                val elementName = record.get("elementName").toString()
                val ed = new ElementDefinition(elementId, elementName)

                val propId = record.get("propId").toString()
                val propName = record.get("propName").toString()
                val propType = record.get("propType").toString()
                val propDescription = record.get("propDescription").toString()
                val pd = new PropertyDefinition(propId, propName, propType, propDescription)
                val pair = (ed, pd)
                results += pair
              })

            records.length shouldBe 2

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

            //The result I want is a List[ElementDefintions]
            val elements:List[ElementDefinition] = elementsMap.values.toList
            elements.length shouldBe 1
            elements(0).properties.length shouldBe 2
        }

        it("should retrieve an ElementDefinition")(pending)
        it("should update an ElementDefinition")(pending)
        it("should delete an ElementDefinition")(pending)
        it("should list all ElementDefintions")(pending)
      }
    }
    describe("User Space"){
      it("should have one and only one user space"){
        val findUserSpace = "match (us:internal_user_space) return us.mid as id, us.name as name"
        val userSpaces = query[UserSpace](engine.database, findUserSpace, null, UserSpace.queryMapper)
        userSpaces.isEmpty shouldBe false
        userSpaces.length shouldBe 1
      }

      describe("Element Definitions"){
        it("should create an ElementDefinition")(pending)
        it("should retrieve an ElementDefinition")(pending)
        it("should update an ElementDefinition")(pending)
        it("should delete an ElementDefinition")(pending)
        it("should list all ElementDefintions")(pending)
      }

      describe("DataSet"){
        it("should create a DataSet")(pending)
        it("should retrieve a DataSet")(pending)
        it("should update a DataSet")(pending)
        it("should delete a DataSet")(pending)
        it("should list all available DataSets")(pending)

        describe("Element Definitions"){
          it("should create an ElementDefinition")(pending)
          it("should retrieve an ElementDefinition")(pending)
          it("should update an ElementDefinition")(pending)
          it("should delete an ElementDefinition")(pending)
          it("should list all ElementDefintions")(pending)
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
}
