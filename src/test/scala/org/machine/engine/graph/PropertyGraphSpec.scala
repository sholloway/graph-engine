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

class PropertyGraphSpec extends FunSpec
  with Matchers
  with EasyMockSugar
  with BeforeAndAfterAll{
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

  describe("Property Graph"){
    it("should have graph constraints created"){
      val foundConstraints:Array[String] = query[String](engine.database,
        "CALL db.constraints()",
        null,
        { (results, record) =>
            val constrainDescription:String = record.get("description").toString()
            results += constrainDescription
          }
      )
      foundConstraints.length should equal(12)
    }

    it("should have the system space created"){
      val systemSpaces:Array[SystemSpace] = query[SystemSpace](engine.database,
        "match (ss:internal_system_space) return ss.mid as id, ss.name as name",
        null,
        SystemSpace.queryMapper)
      systemSpaces.length should equal(1)
    }

    it("should have the system element definitions created")(pending)
  }
}
