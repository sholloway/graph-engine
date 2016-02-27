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

class EditElementPropertyDefinition(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  val filter = List("mid", "pname")

  def execute() = {
    logger.debug("EditElementPropertyDefinition: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editPropertyDefinition(graphDB)
    })
  }

  private def emptyResultProcessor(results: ArrayBuffer[UserSpace],
    record: java.util.Map[java.lang.String, Object]) = { }

  private def editPropertyDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementPropertyDefinition: Editing property definition.")
    val editPropertyDefinitionStatement = buildStatement()
    insert( graphDB,
      editPropertyDefinitionStatement,
      commandOptions,
      emptyResultProcessor)
  }

  private def buildStatement():String = {
    val setClause = buidSetClause(commandOptions)
    val editPropertyDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition {name:{pname}})
    |set setClause, pd.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", cmdScope.scope)
       .replaceAll("setClause", setClause)
     return editPropertyDefinitionStatement
  }

  private def buidSetClause(commandOptions:Map[String, AnyRef]):String = {
    val clause = new StringBuilder()
    commandOptions.keys.foreach(k => {
      if(!filter.contains(k)){
        clause append "pd.%s = {%s}\n".format(k,k)
      }
    })
    return clause.lines.mkString(", ")
  }
}
