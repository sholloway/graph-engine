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

class DeleteAssociation(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4DeleteCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():DeleteCmdResult[String] = {
    logger.debug("DeleteAssociation: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteAssociation(graphDB)
    })
    return DeleteCmdResult(cmdOptions.option[String]("associationId"))
  }

  private def deleteAssociation(graphDB: GraphDatabaseService):Unit = {
    logger.debug("DeleteAssociation: Deleting association.")
    val statement = """
    |match (x)-[association {associationId:{associationId}}]->(y)
    |delete association
    """.stripMargin
    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Association])
  }
}
