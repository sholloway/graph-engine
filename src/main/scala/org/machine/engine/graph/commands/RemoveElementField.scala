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

class RemoveElementField(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4DeleteCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():DeleteCmdResult[String] = {
    logger.debug("RemoveElementField: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      removeField(graphDB)
    })
    return DeleteCmdResult(cmdOptions.option[String]("elementId"))
  }

  /*FIXME Expand to also support dataset name for the filter.*/
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
