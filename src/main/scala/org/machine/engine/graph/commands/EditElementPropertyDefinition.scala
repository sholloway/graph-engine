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

  def execute():String = {
    logger.debug("EditElementPropertyDefinition: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editPropertyDefinition(graphDB)
    })
    val mid = commandOptions.get("mid").getOrElse(throw new InternalErrorException("mid required"))
    return mid.toString
  }

  private def editPropertyDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementPropertyDefinition: Editing property definition.")
    val editPropertyDefinitionStatement = buildStatement()
    run( graphDB,
      editPropertyDefinitionStatement,
      commandOptions,
      emptyResultProcessor[PropertyDefinition])
  }

  private def buildStatement():String = {
    val setClause = buildSetClause("pd", commandOptions.toMap, filter)
    val scope = buildScope(cmdScope, commandOptions)
    val editPropertyDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition {name:{pname}})
    |set setClause, pd.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", scope)
       .replaceAll("setClause", setClause)
     return editPropertyDefinitionStatement
  }

  private def buildScope(datScope:CommandScope, options:Map[String, AnyRef]):String = {
    val scope = datScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => CommandScopes.UserSpaceScope.scope
      case CommandScopes.DataSetScope => {
        var filter:String = null
        if(options.contains("dsId")){
          filter = "%s {mid:{dsId}}".format(CommandScopes.DataSetScope.scope)
        }else if(options.contains("dsName")){
          filter = "%s {name:{dsName}}".format(CommandScopes.DataSetScope.scope)
        }else{
          val msg = """
          |EditElementPropertyDefinition requires that dsId or dsName is provided on
          |commandOptions when the scope is of type CommandScopes.DataSet.
          """.stripMargin
          throw new InternalErrorException(msg)
        }
        filter
      }
    }
    return scope
  }
}
