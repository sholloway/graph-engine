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

class RemoveAssociationField(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("RemoveAssociationField: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      removeField(graphDB)
    })
    return cmdOptions.option[String]("associationId")
  }

  private def removeField(graphDB: GraphDatabaseService):Unit = {
    logger.debug("RemoveAssociationField: Removing Field.")
    val prefix = "association"
    val exclude = List("dsId", "associationId")
    val removeClause = buildRemoveClause(prefix, cmdOptions.keys, exclude)
    val statement = """
    |match (x)-[association]->(y)
    |where association.associationId = {associationId}
    |remove removeClause
    """.stripMargin
      .replaceAll("removeClause", removeClause)
    logger.debug(statement)
    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Element])
  }
}
