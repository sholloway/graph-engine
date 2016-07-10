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


class ElementManagementSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  var engine:Engine = null
  var notesDataSetId:String = null
  var noteElementDefininitionId:String = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    notesDataSetId = engine.createDataSet("notes", "My collection of notes.")
    noteElementDefininitionId =
      engine
        .onDataSet(notesDataSetId)
        .defineElement("note", "short piece of text")
          .withProperty("title", "String", "The title of the note.")
          .withProperty("description", "String", "An optional description of the note.")
          .withProperty("body", "String", "The body of the note.")
      .end
  }

  override def afterAll(){
    perge
  }

  describe("Machine Engine"){
    describe("DataSet"){
      describe("Element Instances"){
        it("should create an Element"){
          val noteTxt = """
          |This is a short little note on the meaning of life.
          |Haven't found any meaning yet...
          """.stripMargin

          val noteId =
            engine
              .onDataSet(notesDataSetId)
              .provision(noteElementDefininitionId)
                .withField("title", "observations")
                .withField("description", "My observations")
                .withField("body", noteTxt)
            .end

          val note =
            engine
              .onDataSet(notesDataSetId)
              .findElement(noteId)

          note.elementType should equal("note")
          note.fields should have size 3

          note.field[String]("title") should equal("observations")
          note.field[String]("description") should equal("My observations")
          note.field[String]("body") should equal(noteTxt)
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

        it("should update an Element's fields"){
          val noteId =
            engine
              .onDataSet(notesDataSetId)
              .provision(noteElementDefininitionId)
                .withField("title", "Funny Thing Happened Today")
                .withField("description", "Note about earlier today.")
                .withField("body", "I noticed a John had two cups of coffee.")
            .end

          val updatedBody = """
          I noticed a John had two cups of coffee.
          John never haves two cups of coffee.
          """.stripMargin

          engine
            .onDataSet(notesDataSetId)
            .onElement(noteId)
            .setField("body", updatedBody)
          .end

          val noteElement =
            engine
              .onDataSet(notesDataSetId)
              .findElement(noteId)

          noteElement.field[String]("body") should equal(updatedBody)
        }

        it ("should update multiple fields on an Element"){
          val noteId =
            engine
              .onDataSet(notesDataSetId)
              .provision(noteElementDefininitionId)
                .withField("title", "Blah")
                .withField("description", "")
                .withField("body", "")
            .end

          val updatedTitle = "Colors that strike my fancy"
          val updatedDesc = "Colors that I enjoy"
          val updatedBody = "Blue, light blue, and robin blue."

          engine
            .onDataSet(notesDataSetId)
            .onElement(noteId)
            .setField("title", updatedTitle)
            .setField("description", updatedDesc)
            .setField("body", updatedBody)
          .end

          val noteElement =
            engine
              .onDataSet(notesDataSetId)
              .findElement(noteId)

          noteElement.field[String]("title") should equal(updatedTitle)
          noteElement.field[String]("description") should equal(updatedDesc)
          noteElement.field[String]("body") should equal(updatedBody)
        }

        it("should delete an Element"){
          val noteId =
            engine
              .onDataSet(notesDataSetId)
              .provision(noteElementDefininitionId)
                .withField("title", "Poorly thought out note")
                .withField("description", "A note not long for this world.")
                .withField("body", "Thibbbbit")
            .end

          engine
            .onDataSet(notesDataSetId)
            .onElement(noteId)
            .delete
          .end

          val expectedNameMsg = "No element with mid: %s could be found.".format(noteId)
          the [InternalErrorException] thrownBy{
            engine
              .onDataSet(notesDataSetId)
              .findElement(noteId)
          }should have message expectedNameMsg
        }

        it("should remove a field on an Element"){
          val noteId = engine
            .onDataSet(notesDataSetId)
            .provision(noteElementDefininitionId)
            .withField("title", "Poorly thought out note")
            .withField("description", "A note not long for this world.")
            .withField("body", "Thibbbbit")
            .withField("bad_field", 456)
            .withField("another_bad_field", false)
          .end

          engine
            .onDataSet(notesDataSetId)
            .findElement(noteId)
            .fields should have size 5

          engine
            .onDataSet(notesDataSetId)
            .onElement(noteId)
            .removeField("bad_field")
            .removeField("another_bad_field")
          .end

          engine
            .onDataSet(notesDataSetId)
            .findElement(noteId)
            .fields should have size 3
        }
      }
    }
  }
}
