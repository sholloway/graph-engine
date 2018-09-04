package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.nodes._

class CreateLayoutDefinition(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions)extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._
  
  def execute():InsertCmdResult[String] = {
    logger.debug("CreateLayoutDefinition: Executing Command")
    generateId(cmdOptions)
    transaction(database, (graphDB:GraphDatabaseService) => {
      createLayoutDefinition(graphDB)
    })
    val mid = cmdOptions.option[String]("mid")
    return InsertCmdResult(mid.toString)
  }

  private def createLayoutDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateLayoutDefinition: Creating a new layout definition.")
    cmdOptions.addOption("creationTime", time)
    val createLayoutDefStatement = """
      |match (s:internal_system_space)
      |create (s)-[:exists_in]->(ld:layout_definition
      |{
      |  mid: {mid},
      |  name: {name},
      |  description: {description},
      |  creation_time: {creationTime}
      |})
      """.stripMargin

    run( graphDB,
      createLayoutDefStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[LayoutDefinition])
  }
}
