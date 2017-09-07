package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class EditElement(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4UpdateCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():UpdateCmdResult[String] = {
    logger.debug("EditDataSet: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editDataSet(graphDB)
    })
    return UpdateCmdResult(cmdOptions.option[String]("elementId"))
  }

  private def editDataSet(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElement: Editing element definition.")
    val prefix = "e"
    val exclude = List("mid", "dsId", "elementId")
    val setClause = buildSetClause(prefix, cmdOptions.keys, exclude)
    val editDataSetStatement = """
    |match (ds:data_set {mid:{dsId}})-[:contains]->(e {mid:{elementId}})
    |set setClause, ds.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("setClause", setClause)

    run( graphDB,
      editDataSetStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Element])
  }
}
