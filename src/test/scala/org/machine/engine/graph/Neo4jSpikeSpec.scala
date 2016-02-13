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
import scala.collection.mutable.ArrayBuffer

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

  class Book(id: Long, title: String){
    override def toString():String = {
      return id.toString() + " " + title
    }
  }

  def bookMapper(results: ArrayBuffer[Book], record: java.util.Map[java.lang.String, Object]):Unit = {
    val id = record.get("id")
    val title = record.get("title")
    results += new Book(id.asInstanceOf[Long], title.toString())
  }

  describe("Neo4J Database Integration"){
    it ("should persist and query nodes"){
      val db = graphDBOption.getOrElse(null)
      transaction(db, (graphDB:GraphDatabaseService)=>{
        //Preferable to use Label Enums if the Label is known ahead of time.
        val bookLabel = DynamicLabel.label("book")

        val aBook: Node = graphDB.createNode()
        aBook.setProperty("title", "The Left Hand of Darkness")
        aBook.addLabel(bookLabel)

        val anotherBook: Node = graphDB.createNode()
        anotherBook.setProperty("title", "Neuromancer")
        anotherBook.addLabel(bookLabel)

        val finalBook: Node = graphDB.createNode()
        finalBook.setProperty("title", "Childhood's End")
        finalBook.addLabel(bookLabel)
      })

      val cypher = "match (b:book) return b.title as title, id(b) as id"
      val books = query[Book](db, cypher, bookMapper)

      books.length shouldBe(3)
    }

    it ("should associate nodes")(pending)
  }
}
