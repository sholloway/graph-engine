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

        modifiedDS.lastModifiedTime.length should be > 0
      }

      it("should delete a DataSet")(pending)

      it("should list all available DataSets"){
        engine
          .createDataSet("A", "Empty Data Set")

        engine
          .createDataSet("B", "Empty Data Set")

        engine
          .createDataSet("C", "Empty Data Set")

        engine
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

          engine
            .createDataSet(datasetName, "A collection of business capabilities.")

          engine
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

      describe("Element Instances"){
        it("should create an Element"){
          val dsId =
            engine
              .createDataSet("ZZ", "")

          val edId =
            engine
              .onDataSet(dsId)
              .defineElement("note", "short piece of text")
                .withProperty("title", "String", "The title of the note.")
                .withProperty("description", "String", "An optional description of the note.")
                .withProperty("body", "String", "The body of the note.")
            .end

          val noteId =
            engine
              .onDataSet(dsId)
              .provision(edId)
                .withField("title", "observations")
                .withField("description", "My observations")
                .withField("body", "This is a short little note on the meaning of life.")

            .end

          val note =
            engine
              .onDataSet(dsId)
              .findElement(noteId)

          note.elementType should equal("note")
          note.fields should have size 3

          note.field[String]("title") should equal("observations")
          note.field[String]("description") should equal("My observations")
          note.field[String]("body") should equal("This is a short little note on the meaning of life.")
        }

        it("should store and retrieve elements with Java primative types"){
          val dsId =
            engine
              .createDataSet("ZZ", "")

          val edId =
            engine
              .onDataSet(dsId)
              .defineElement("example", "a node that contains every possible data type.")
                .withProperty("bool", "Boolean", "A boolean value. true/false")
                .withProperty("byte", "Byte", "8-bit integer. -128 to 127. Inclusive")
                .withProperty("short", "Short", "16-bit integer. -32,768 to 32,767. Inclusive")
                .withProperty("int", "Int", "32-bit integer. -2,147,483,648 to 2,147,483,647. Inclusive")
                .withProperty("long", "Long", "64-bit integer. -9,223,372,036,854,775,808 to 9223372,036,854,775,807. Inclusive")
                .withProperty("float", "Float", "32-bit IEEE 754 floating-point number.")
                .withProperty("double", "Double", "64-bit IEEE 754 floating-point number.")
                .withProperty("char", "Char", "16-bit unsigned integers representing Unicode characters. u0000 to uffff (0 to 65535)")
                .withProperty("string", "String", "Sequence of Unicode characters.")
            .end

          val i:Int = 123
        	val b:Byte = 123
        	val s:Short = 123
        	val l:Long = 123L
        	val f:Float = 123.10f
        	val d:Double = 123.10d
        	val c:Char = 'A'
        	val str:String = "a string"

          val exampleId =
            engine
              .onDataSet(dsId)
              .provision(edId)
                .withField("integer", i)
                .withField("byte", b)
                .withField("short", s)
                .withField("long", l)
                .withField("float", f)
                .withField("double", d)
                .withField("char", c)
                .withField("string", str)
            .end

          val element =
            engine
              .onDataSet(dsId)
              .findElement(exampleId)

          element.elementType should equal("example")
          element.fields should have size 8

          element.field[Int]("integer") should equal(i)
          element.field[Byte]("byte") should equal(b)
          element.field[Short]("short") should equal(s)
          element.field[Long]("long") should equal(l)
          element.field[Float]("float") should equal(f)
          element.field[Double]("double") should equal(d)
          element.field[Char]("char") should equal(c)
          element.field[String]("string") should equal(str)
        }

        ignore("should store and retrieve elements with arrays of Java primatives"){
          val dsId =
            engine
              .createDataSet("ZZ", "")

          val edId =
            engine
              .onDataSet(dsId)
              .defineElement("array example", "a node that contains every possible data type.")
              .withProperty("bool_array", "Boolean[]", "An array of boolean values")
              .withProperty("byte_array", "byte[]", "Array of bytes.")
              .withProperty("short_array", "Short[]", "Array of shorts.")
              .withProperty("int_array", "Int[]", "Array of integers.")
              .withProperty("long_array", "Long[]", "Array of longs.")
              .withProperty("float_array", "Float[]", "Array of floats.")
              .withProperty("double_array", "Double[]", "Array of doubles.")
              .withProperty("char_array", "Char[]", "Array of characters.")
              .withProperty("string_array", "String[]", "Array of strings.")
            .end

            val doubleArray:Array[Double] = Array(1d,2d,3d)
            val exampleId =
              engine
                .onDataSet(dsId)
                .provision(edId)
                  .withField("double_array", doubleArray)
              .end
        }

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
