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

class RemoveElementField(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("RemoveElementField: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      removeField(graphDB)
    })
    return cmdOptions.option[String]("elementId")
  }

  //BUG: This needs to be scoped to be inside the dataset.
  //(ds:internal_data_set)-[:contains]->(e) is not working.
  //Probably a bug with Create Element.
  //|match (ds:internal_data_set {mid:{dsId}})-[:contains]->(startingElement {mid:{startingElementId}})
  private def removeField(graphDB: GraphDatabaseService):Unit = {
    logger.debug("RemoveElementField: Removing Field.")
    val prefix = "e"
    val exclude = List("dsId", "elementId")
    val removeClause = buildRemoveClause(prefix, cmdOptions.keys, exclude)
    val statement = """
    |match (ds:internal_data_set {mid:{dsId}})-[:contains]->(e {mid:{elementId}})
    |remove removeClause
    |return e.mid as mid
    """.stripMargin
      .replaceAll("removeClause", removeClause)
    logger.debug(statement)
    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      resultProcessor[Element])
  }

  def resultProcessor[T](results: ArrayBuffer[T], record: java.util.Map[java.lang.String, Object]) = {
    record.keys.foreach(k => {
      logger.debug("%s: %s".format(k, record.get(k).toString))
    })
  }
}
