package org.machine.engine.graph.commands

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}

import org.machine.engine.logger._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class EditDataSet(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("EditDataSet: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editDataSet(graphDB)
    })
    return cmdOptions.option[String]("dsId")
  }

  private def editDataSet(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditDataSet: Editing element definition.")
    val setClause = buildSetClause("ds", cmdOptions.keys, List("mid"))
    val editDataSetStatement = """
    |match (us:internal_user_space)-[:contains]->(ds:internal_data_set {mid:{dsId}})
    |set setClause, ds.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("setClause", setClause)

    run( graphDB,
      editDataSetStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[DataSet])
  }
}