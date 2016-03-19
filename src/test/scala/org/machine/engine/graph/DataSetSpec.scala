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
    }
  }
}
