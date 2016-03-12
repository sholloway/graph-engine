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

class EditElementDefintion(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute() = {
    logger.debug("EditElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editElementDefinition(graphDB)
    })
  }

  private def editElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementDefintion: Editing element definition.")
    val setClause = buidSetClause(commandOptions)
    val editElementDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |set setClause, ed.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", cmdScope.scope)
       .replaceAll("setClause", setClause)

    run( graphDB,
      editElementDefinitionStatement,
      commandOptions,
      emptyResultProcessor[ElementDefinition])
  }

  private def buidSetClause(commandOptions:Map[String, AnyRef]):String = {
    val clause = new StringBuilder()
    commandOptions.keys.foreach(k => {
      if(k != "mid"){
        clause append "ed.%s = {%s}\n".format(k,k)
      }
    })
    return clause.lines.mkString(", ")
  }
}
