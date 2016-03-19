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

class DeleteElement(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("DeleteElement: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteElement(graphDB)
    })
    return cmdOptions.option[String]("elementId")
  }

  private def deleteElement(graphDB: GraphDatabaseService):Unit = {
    logger.debug("DeleteElement: Deleting element.")
    val statement = """
    |match (ds:internal_data_set {mid:{dsId}})-[:contains]->(e {mid:{elementId}})
    |detach delete e
    """.stripMargin

    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Element])
  }
}
