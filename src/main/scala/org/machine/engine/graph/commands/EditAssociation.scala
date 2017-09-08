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

class EditAssociation(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4UpdateCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():UpdateCmdResult[String] = {
    logger.debug("EditAssociation: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editAssociation(graphDB, cmdOptions)
    })
    return UpdateCmdResult(cmdOptions.option[String]("associationId"))
  }

  private def editAssociation(graphDB:GraphDatabaseService,
    cmdOptions: GraphCommandOptions):Unit = {
    logger.debug("EditAssociation: Editing association.")
    val prefix = "association"
    val exclude = List("dsId", "associationId", "activeUserId")
    val setClause = buildSetClause(prefix, cmdOptions.keys, exclude)
    val statement = """
    |match (x)-[association {associationId:{associationId}}]->(y)
    |set setClause, association.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("setClause", setClause)
    logger.debug(statement)
    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Association])
  }
}
