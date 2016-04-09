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

class RemoveOutboundAssociations(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  associationIds: List[String]) extends LazyLogging{
  import Neo4JHelper._

  def execute():Unit = {
    logger.debug("RemoveOutboundAssociations: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteAssociation(graphDB, cmdOptions)
    })
  }

  private def deleteAssociation(graphDB: GraphDatabaseService, cmdOptions: GraphCommandOptions):Unit = {
    logger.debug("RemoveOutboundAssociations: Deleting association.")
    val statement = """
    |match (x)-[association]->(y)
    |where association.associationId in {existingOutboundAssociationIds}
    | and x.mid = {elementId}
    |delete association
    """.stripMargin
    val options = cmdOptions.toJavaMap
    options.put("existingOutboundAssociationIds", associationIds)
    run( graphDB,
      statement,
      options,
      emptyResultProcessor[Association])
  }
}
