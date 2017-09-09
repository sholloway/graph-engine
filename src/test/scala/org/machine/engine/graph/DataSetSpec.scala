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


class DataSetSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterEach{
  import Neo4JHelper._
  import TestUtils._
  private var engine:Engine = null
  private var activeUserId:String = null

  override def beforeEach(){
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

  override def afterEach(){
    perge
  }

  describe("Machine Engine"){
    describe("DataSet"){
      it("should create a DataSet"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.forUser(activeUserId).createDataSet(datasetName, datasetDescription)
        val dsOption = engine.forUser(activeUserId).datasets().find(ds => {ds.name == datasetName})
        dsOption.get should have(
          'name (datasetName),
          'description (datasetDescription)
        )
      }

      it("should retrieve a DataSet by name"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.forUser(activeUserId).createDataSet(datasetName, datasetDescription)
        val ds = engine.forUser(activeUserId).findDataSetByName(datasetName)
        ds should have(
          'name (datasetName),
          'description (datasetDescription)
        )
      }

      it("should update both name & defintion"){
        val datasetName = "Capability Definitions"
        val datasetDescription = "A collection of business capabilities."
        engine.forUser(activeUserId).createDataSet(datasetName, datasetDescription)
        val ds = engine.forUser(activeUserId).findDataSetByName(datasetName)

        val newName = "Capability Definitions: V1"
        val newDescription = "Version 1 of business capabilities definitions."
        engine.forUser(activeUserId)
          .onDataSet(ds.id)
          .setName(newName)
          .setDescription(newDescription)
        .end

        val modifiedDS = engine.forUser(activeUserId).findDataSetById(ds.id)

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
        engine.forUser(activeUserId)
          .createDataSet("A", "Empty Data Set")

        engine.forUser(activeUserId)
          .createDataSet("B", "Empty Data Set")

        engine.forUser(activeUserId)
          .createDataSet("C", "Empty Data Set")

        engine.forUser(activeUserId)
          .createDataSet("D", "Empty Data Set")

        engine.forUser(activeUserId).datasets().length should be >= 4
      }
    }
  }
}
