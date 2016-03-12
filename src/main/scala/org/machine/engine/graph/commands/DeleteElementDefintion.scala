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

class DeleteElementDefintion(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute() = {
    logger.debug("DeleteElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      deleteAssociatedPropertyDefinitions(graphDB)
      deleteElementDefinition(graphDB)
    })
  }

  private def deleteAssociatedPropertyDefinitions(graphDB:GraphDatabaseService):Unit = {
    logger.debug("DeleteElementDefintion: Deleting associated property definitions")
    val statement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition)
    |detach delete pd
    """.stripMargin
       .replaceAll("space", cmdScope.scope)

    run( graphDB,
      statement,
      commandOptions,
      emptyResultProcessor[ElementDefinition])
  }

  private def deleteElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("DeleteElementDefintion: Deleting element definition.")
    val statement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |detach delete ed
    """.stripMargin
       .replaceAll("space", cmdScope.scope)

    run( graphDB,
      statement,
      commandOptions,
      emptyResultProcessor[ElementDefinition])
  }
}
