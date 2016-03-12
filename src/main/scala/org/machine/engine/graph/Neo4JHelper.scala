package org.machine.engine.graph

import org.neo4j.graphdb._
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import org.machine.engine.exceptions._

/** A mixin for making working with Neo4J easier.
*/
object Neo4JHelper{
  val NOT_FOUND_VALUE = ""

  /** A wrapper function for executing Neo4J operations inside a database transaction.

    Use:
    transaction(db, (graphDB: GraphDatabaseService) => {
      val aBook: Node = graphDB.createNode()
      aBook.setProperty("title", "The Left Hand of Darkness")
      aBook.addLabel(LibraryLabels.Book)
    })

    Or
    val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
    var systemSpaces:Array[SystemSpace] = null
    transaction(db, (graphDB: GraphDatabaseService) =>{
      systemSpaces = insert[SystemSpace](graphDB,
        CreateSystemSpaceCypherStatement,
        createSystemSpaceParams,
        SystemSpace.queryMapper)
    })

    @param graphDB: The Neo4J client.
    @param tx: Function to execute inside the scope of the transaction.
  */
  def transaction(graphDB:GraphDatabaseService, tx:GraphDatabaseService => Unit):Unit = {
    var dbTransactionOption: Option[Transaction] = None
    try{
      dbTransactionOption = Some(graphDB.beginTx());
      tx(graphDB)
      dbTransactionOption.foreach(t => t.success())
    }catch{
      case e:Throwable => e.printStackTrace
    }finally{
      dbTransactionOption.foreach(tx => tx.close())
    }
  }

  /** Generic query function.

    Use:
    val books = query[Book](db, cypher,
      (results:ArrayBuffer[Book],
        record: java.util.Map[java.lang.String, Object]) => {
      val id = record.get("id")
      val title = record.get("title")
      results += new Book(id.asInstanceOf[Long], title.toString())
    })

    @param graphDB: The Neo4J client.
    @param query: The cypher statement to execute.
    @param params: The parameters for the cypher statement.
    @param recordHandler: The function to execute for handling result mapping.
  */
  def query[T:Manifest](graphDB: GraphDatabaseService,
    query: String,
    params: java.util.Map[java.lang.String, Object],
    recordHandler: (ArrayBuffer[T],
      java.util.Map[java.lang.String, Object]) => Unit):Array[T] = {
    val queryParamsOption: Option[java.util.Map[java.lang.String, Object]] = Option(params).orElse(None)
    var dbTransactionOption: Option[Transaction] = None
    var resultOption: Option[Result] = None
    var results = new ArrayBuffer[T]()
    try{
      dbTransactionOption = Some(graphDB.beginTx());
      if (queryParamsOption.isDefined){
        resultOption = Some(graphDB.execute(query, queryParamsOption.get))
      }else{
        resultOption = Some(graphDB.execute(query))
      }
      while(resultOption.get.hasNext()){
        val record = resultOption.get.next()
        recordHandler(results, record)
      }
    }catch{
      case e:Throwable => e.printStackTrace
    }finally{
      resultOption.foreach(_.close())
      dbTransactionOption.foreach(_.close())
    }
    return results.toArray
  }

  /** Executes a cypher statement inside of a transaction.

    Use:
    val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
    var systemSpaces:Array[SystemSpace] = null
    transaction(db, (graphDB: GraphDatabaseService) =>{
      systemSpaces = insert[SystemSpace](graphDB,
        CreateSystemSpaceCypherStatement,
        createSystemSpaceParams,
        SystemSpace.queryMapper)
    })

    @param graphDB: The Neo4J client.
    @param statement: The cypher statement to execute.
    @param params: The parameters for the cypher statement.
    @param recordHandler: The function to execute for handling result mapping.
  */
  def run[T:Manifest](graphDB: GraphDatabaseService,
    statement: String,
    params: java.util.Map[java.lang.String, Object],
    recordHandler: (ArrayBuffer[T],
      java.util.Map[java.lang.String, Object]) => Unit):Array[T] = {
    val statementParamsOption: Option[java.util.Map[java.lang.String, Object]] = Option(params).orElse(None)
    var resultOption: Option[Result] = None
    var results = new ArrayBuffer[T]()
    try{
      if (statementParamsOption.isDefined){
        resultOption = Some(graphDB.execute(statement, statementParamsOption.get))
      }else{
        resultOption = Some(graphDB.execute(statement))
      }
      while(resultOption.get.hasNext()){
        val record = resultOption.get.next()
        recordHandler(results, record)
      }
    }catch{
      case e:Throwable => e.printStackTrace
    }finally{
      resultOption.foreach(_.close())
    }
    return results.toArray
  }

  /** Generates a Unique Universal Identifier
  */
  def uuid = java.util.UUID.randomUUID.toString

  /** Given a record from a query result set, finds a value corrisponding to
  *   the provided column name. Thows an exception if the field is not found
  *   when required is set to true.
  *
  * @param name: The name of the field to find.
  * @param record: The record to map a value from.
  * @param required: Specifies if an error should be thrown if the desired field is not found.
  */
  def mapString(name:String,
    record:java.util.Map[java.lang.String, Object],
    required: Boolean):String = {
    var response:String = null
    if (record.containsKey(name) && record.get(name) != null){
      response = record.get(name).toString()
    }else if(!record.containsKey(name) && required){
      val msg = "The required field: %s was not found in the query response.".format(name)
      throw new InternalErrorException(msg)
    }else{
      response = NOT_FOUND_VALUE
    }
    return response
  }
}
