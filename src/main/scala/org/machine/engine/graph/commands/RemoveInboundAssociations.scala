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

class RemoveInboundAssociations(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  associationIds: List[String],
  logger: Logger){
  import Neo4JHelper._

  def execute():Unit = {
    logger.debug("RemoveInboundAssociations: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteAssociation(graphDB, cmdOptions)
    })
  }

  private def deleteAssociation(graphDB: GraphDatabaseService, cmdOptions: GraphCommandOptions):Unit = {
    logger.debug("RemoveInboundAssociations: Deleting association.")
    val statement = """
    |match (x)-[association]->(y)
    |where association.associationId in {existingInBoundAssociationIds}
    | and y.mid = {elementId}
    |delete association
    """.stripMargin
    val options = cmdOptions.toJavaMap
    options.put("existingInBoundAssociationIds", associationIds)
    run( graphDB,
      statement,
      options,
      emptyResultProcessor[Association])
  }
}
