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

class DeleteElement(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4DeleteCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():DeleteCmdResult[String] = {
    logger.debug("DeleteElement: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteElement(graphDB)
    })
    return DeleteCmdResult(cmdOptions.option[String]("elementId"))
  }

  private def deleteElement(graphDB: GraphDatabaseService):Unit = {
    logger.debug("DeleteElement: Deleting element.")
    val statement = """
    |match (ds:data_set {mid:{dsId}})-[:contains]->(e {mid:{elementId}})
    |detach delete e
    """.stripMargin

    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Element])
  }
}
