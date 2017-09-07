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
  cmdOptions:GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateDataSet: Executing Command")
    generateId(cmdOptions)
    transaction(database, (graphDB:GraphDatabaseService) => {
      createDataSet(graphDB)
    })
    val mid = cmdOptions.option[String]("mid")
    return InsertCmdResult(mid.toString)
  }

  /*
  TODO Update data sets to be relative to a user.
  - Merge the createDataset and registerDataSet to be a single command
  - Change the "contains" relationship to "owns"
  match (u:user) where u.mid = {activeUserId}
  create (u)->[owns]->(d:data_set){
    mid:{mid},
    name:{name},
    description:{description},
    creation_time:timestamp(),
    last_modified_time:timestamp()
  }
  */
  private def createDataSet(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateDataSet: Creating data set.")
    val createDataSetStatement = """
      |match (u:user) where u.mid = {activeUserId}
      |create (u)-[:owns]->(d:data_set{
      |  mid:{mid},
      |  name:{name},
      |  description:{description},
      |  creation_time:timestamp(),
      |  last_modified_time:timestamp()
      |})
      """.stripMargin

    run(graphDB,
      createDataSetStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[DataSet])
  }
}
