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

class RemoveElementPropertyDefinition(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  val filter = List("mid", "pname")

  def execute() = {
    logger.debug("RemoveElementPropertyDefinition: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      removePropertyDefinition(graphDB)
    })
  }

  private def emptyResultProcessor(results: ArrayBuffer[UserSpace],
    record: java.util.Map[java.lang.String, Object]) = { }

  private def removePropertyDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("RemoveElementPropertyDefinition: Editing property definition.")
    val removePropertyDefinitionStatement = buildStatement()
    run( graphDB,
      removePropertyDefinitionStatement,
      commandOptions,
      emptyResultProcessor)
  }

  private def buildStatement():String = {
    return """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition {name:{pname}})
    |detach delete pd
    """.stripMargin
       .replaceAll("space", cmdScope.scope)
  }
}
