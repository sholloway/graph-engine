package org.machine.engine.graph.commands

import com.typesafe.scalalogging._

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class CreateDataSet(database:GraphDatabaseService,
  cmdScope:CommandScope,
  cmdOptions:GraphCommandOptions) extends Neo4JCommand with LazyLogging{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("CreateDataSet: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      createDataSet(graphDB)
      registerDataSet(graphDB)
    })
    val mid = cmdOptions.option[String]("mid")
    return mid.toString
  }

  private def createDataSet(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateDataSet: Creating data set.")
    val createDataSetStatement = """
      |merge(ds:internal_data_set
      |{
      |  name:{name}
      |})
      |on create set ds.creation_time = timestamp()
      |on create set ds.mid = {mid}
      |on create set ds.description = {description}
      |on match set ds.last_modified_time = timestamp()
      """.stripMargin

    run( graphDB,
      createDataSetStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[DataSet])
  }

  private def registerDataSet(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateDataSet: Associating the data set to the scoped space.")
    val associateToScopedSpace = """
      |match (ss:label)
      |match (ds:internal_data_set) where ds.mid = {mid}
      |merge (ss)-[:contains]->(ds)
      """.stripMargin.replaceAll("label", cmdScope.scope)
      run(graphDB,
        associateToScopedSpace,
        cmdOptions.toJavaMap,
        emptyResultProcessor[DataSet])
  }
}
