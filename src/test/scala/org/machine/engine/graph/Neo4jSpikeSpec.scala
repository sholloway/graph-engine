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

import org.machine.engine.{Engine, TestUtils}

class Neo4jSpikeSpec extends FunSpecLike with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._
  import TestUtils._
  var engine:Engine = null

  override def beforeAll(){
    engine = Engine.getInstance
    perge
    cleanup
  }

  override def afterAll(){
    perge
    cleanup
  }

  class Book(_id: Long, _title: String){
    def id = this._id
    def title = this._title
    override def toString():String = {
      return id.toString() + " " + title
    }
  }

  class Author(_id: Long, _name: String){
    def id = this._id
    def name = this._name
    override def toString():String = {
      return id.toString() + " " + name
    }
  }

  def bookMapper(results: ArrayBuffer[Book], record: java.util.Map[java.lang.String, Object]):Unit = {
    if(record != null){
      val id = record.get("id")
      val title = record.get("title")
      results += new Book(id.asInstanceOf[Long], title.toString())
    }
  }

  def authorMapper(results: ArrayBuffer[Author], record: java.util.Map[java.lang.String, Object]):Unit = {
    val id = record.get("id")
    val name = record.get("name")
    results += new Author(id.asInstanceOf[Long], name.toString())
  }

  describe("Neo4J Database Integration"){
    it ("should persist and query nodes"){
      transaction(engine.database, (graphDB: GraphDatabaseService) => {
        val aBook: Node = graphDB.createNode()
        aBook.setProperty("title", "The Left Hand of Darkness")
        aBook.addLabel(LibraryLabels.Book)

        val anotherBook: Node = graphDB.createNode()
        anotherBook.setProperty("title", "Neuromancer")
        anotherBook.addLabel(LibraryLabels.Book)

        val finalBook: Node = graphDB.createNode()
        finalBook.setProperty("title", "Childhood's End")
        finalBook.addLabel(LibraryLabels.Book)
      })

      val cypher = "match (b:Book) return b.title as title, id(b) as id"
      val books = query[Book](engine.database, cypher, null, bookMapper)

      books.length shouldBe(3)
    }

    /*Get queries with parameters working...*/
    it ("should associate nodes"){
      transaction(engine.database, (graphDB:GraphDatabaseService)=>{
        val firstBook: Node = graphDB.createNode()
        firstBook.setProperty("title", "The Mote in God's Eye")
        firstBook.addLabel(LibraryLabels.Book)

        val sequel: Node = graphDB.createNode()
        sequel.setProperty("title", "The Gripping Hand")
        sequel.addLabel(LibraryLabels.Book)

        val relationship = sequel.createRelationshipTo(firstBook, DynamicRelationshipType.withName("continued"))
        relationship.setProperty("sequal-date", 1993)
      })

      //Find the sequel
      val cypher = "match (b:Book {title:{title}})-[:continued]-(a:Book) return a.title as title, id(b) as id"
      val params = Map("title"->"The Mote in God's Eye")
      val sequels = query[Book](engine.database, cypher, params, bookMapper)

      sequels.length shouldBe(1)
      sequels(0).title shouldBe("The Gripping Hand")
    }
  }

  private def cleanup() = {
    val delete_books = "match (b:Book) detach delete b"
    val delete_authors = "match (a:Author) detach delete a"
    val map = new java.util.HashMap[java.lang.String, Object]()
    Neo4JHelper.run(engine.database, delete_books, map, Neo4JHelper.emptyResultProcessor[Book])
    Neo4JHelper.run(engine.database, delete_authors, map, Neo4JHelper.emptyResultProcessor[Author])
  }
}
