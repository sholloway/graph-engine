package org.machine.engine.graph

import java.lang.NumberFormatException
import org.neo4j.graphdb._
import org.machine.engine.exceptions._
import org.machine.engine.graph.commands._
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/** A mixin for making working with Neo4J easier.
*/
object Neo4JHelper{
  private val NOT_FOUND_VALUE = ""
  val UNSET_LONG:Long = 0
  val NOT_FOUND_LONG:Long = 0

  /** A wrapper function for executing Neo4J operations inside a database transaction.

    @example
    {{{
    transaction(db, (graphDB: GraphDatabaseService) => {
      val aBook: Node = graphDB.createNode()
      aBook.setProperty("title", "The Left Hand of Darkness")
      aBook.addLabel(LibraryLabels.Book)
    })
    }}}

    @example
    {{{
    val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
    var systemSpaces:Array[SystemSpace] = null
    transaction(db, (graphDB: GraphDatabaseService) =>{
      systemSpaces = insert[SystemSpace](graphDB,
        CreateSystemSpaceCypherStatement,
        createSystemSpaceParams,
        SystemSpace.queryMapper)
    })
    }}}

    @param graphDB The Neo4J client.
    @param tx Function to execute inside the scope of the transaction.
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

  /** Generic query function executed inside a transaction.
    @example
    {{{
    val books = query[Book](db, cypher,
      (results:ArrayBuffer[Book],
        record: java.util.Map[java.lang.String, Object]) => {
      val id = record.get("id")
      val title = record.get("title")
      results += new Book(id.asInstanceOf[Long], title.toString())
    })
    }}}

    @param graphDB The Neo4J client.
    @param query The cypher statement to execute.
    @param params The parameters for the cypher statement.
    @param recordHandler The function to execute for handling result mapping.
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

  /** Executes a cypher statement. Use with [[org.machine.engine.graph.Neo4JHelper.transaction]]
    @example
    {{{
    val createSystemSpaceParams = Map("mid"->uuid, "name"->"System Space")
    var systemSpaces:Array[SystemSpace] = null
    transaction(db, (graphDB: GraphDatabaseService) =>{
      systemSpaces = run[SystemSpace](graphDB,
        CreateSystemSpaceCypherStatement,
        createSystemSpaceParams,
        SystemSpace.queryMapper)
    })
    }}}

    @param graphDB The Neo4J client.
    @param statement The cypher statement to execute.
    @param params The parameters for the cypher statement.
    @param recordHandler The function to execute for handling result mapping.
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
  def uuid() = java.util.UUID.randomUUID.toString

  /** Given a record from a query result set, finds a value corrisponding to
  *   the provided column name. Thows an exception if the field is not found
  *   when required is set to true.
  *
  * @param name The name of the field to find.
  * @param record The record to map a value from.
  * @param required Specifies if an error should be thrown if the desired field is not found.
  * @return The mapped value as a String.
  */
  def mapString(name: String,
    record: java.util.Map[java.lang.String, Object],
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

  /** Given a record from a query result set, finds a value corrisponding to
  *   the provided column name. Thows an exception if the field is not found
  *   when required is set to true. Returns the value of 0 when the field is
  *   not required.
  *
  * @param name The name of the field to find.
  * @param record The record to map a value from.
  * @param required Specifies if an error should be thrown if the desired field is not found.
  * @return The mapped value as a String.
  */
  def mapLong(name: String,
    record: java.util.Map[java.lang.String, Object],
    required: Boolean):Long = {
    var response:Long = UNSET_LONG
    if (record.containsKey(name) && record.get(name) != null){
      val foundValue = record.get(name).toString
      try{
        response = foundValue.toLong
      }catch{
        case e:NumberFormatException => throw new InternalErrorException(s"Failed to parse the expected long: ${foundValue}")
      }
    }else if(!record.containsKey(name) && required){
      val msg = "The required field: %s was not found in the query response.".format(name)
      throw new InternalErrorException(msg)
    }else{
      response = NOT_FOUND_LONG
    }
    return response
  }

  /** Helper function for statements that do not have result statements.
  *
  * @param results The list of records returned by the statement.
  * @param record The record to process.
  */
  def emptyResultProcessor[T](results: ArrayBuffer[T], record: java.util.Map[java.lang.String, Object]) = { }

  /** Helper function for building up a comma seperated list of parameters in a query.
  */
  def buildSetClause(prefix: String,
    keys: List[String],
    exclude: List[String]
  ):String = {
    val clause = new StringBuilder()
    keys.foreach(key => {
      if(!exclude.contains(key)){
        clause append "%s.%s = {%s}\n".format(prefix, key, key)
      }
    })
    return clause.lines.mkString(", ")
  }

  /** Helper function for building up a comma seperated list of parameters in a query.
  */
  def buildRelationshipClause(
    keys: List[String],
    exclude: List[String]
  ):String = {
    val clause = new StringBuilder()
    keys.foreach(key => {
      if(!exclude.contains(key)){
        clause append "%s: {%s}\n".format(key, key)
      }
    })
    return clause.lines.mkString(", ")
  }

  /** Helper function for building up a comma seperated list of keys in a result set.
  */
  def buildFetchClause(prefix: String,
    keys: List[String],
    exclude: List[String]):String = {
    val clause = new StringBuilder()
    keys.foreach(key => {
      if (!exclude.contains(key)){
        clause append "%s.%s as %s\n".format(prefix, key, key)
      }
    })
    return clause.lines.mkString(",\n")
  }

  def buildRemoveClause(prefix: String,
    keys: List[String],
    exclude: List[String]
  ):String = {
    val clause = new StringBuilder()
    keys.foreach(key => {
      if(!exclude.contains(key)){
        clause append "%s.%s\n".format(prefix, key)
      }
    })
    return clause.lines.mkString(", ")
  }

  /** Helper function to calculate the current time. Measured in milliseconds
  since January 1st 1970.
  */
  def time:Long = System.currentTimeMillis
}
