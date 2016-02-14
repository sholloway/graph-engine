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

class Neo4jSpikeSpec extends FunSpec with Matchers with EasyMockSugar with BeforeAndAfterAll{
  import Neo4JHelper._

  val dbPath = "target/simple-db.graph"
  var graphDBOption: Option[GraphDatabaseService] = None

  override def beforeAll(){
    val dbFile = new File(dbPath)
    FileUtils.deleteRecursively(dbFile)
    val graphDBFactory = new GraphDatabaseFactory()
    val graphDB = graphDBFactory.newEmbeddedDatabase(dbFile)
    graphDBOption = Some(graphDB)
  }

  override def afterAll(){
    graphDBOption.foreach(graphDB => graphDB.shutdown())
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
      val db = graphDBOption.getOrElse(null)
      transaction(db, (graphDB: GraphDatabaseService) => {
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
      val books = query[Book](db, cypher, null, bookMapper)

      books.length shouldBe(3)
    }

    /*Get queries with parameters working...*/
    it ("should associate nodes"){
      val db = graphDBOption.getOrElse(null)
      transaction(db, (graphDB:GraphDatabaseService)=>{
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
      val sequels = query[Book](db, cypher, params, bookMapper)

      sequels.length shouldBe(1)
      sequels(0).title shouldBe("The Gripping Hand")
    }


    it ("should be able to map queries of multiple node types")(pending)

    //beyound just Neo4J...
    it("should convert a google protobuf object to a node")(pending)
    it("should have a CQRS persist command")(pending)
    it("should have a CQRS query command")(pending)
  }
}
