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

class RemoveElementPropertyDefinition(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  val filter = List("mid", "pname")

  def execute():String = {
    logger.debug("RemoveElementPropertyDefinition: Executing Command")
    if (!cmdOptions.contains("mid")){
      throw new InternalErrorException("mid required")
    }

    transaction(database, (graphDB:GraphDatabaseService) => {
      removePropertyDefinition(graphDB)
    })
    return cmdOptions.option[String]("mid")
  }

  private def removePropertyDefinition(graphDB: GraphDatabaseService):Unit = {
    logger.debug("RemoveElementPropertyDefinition: Editing property definition.")
    val removePropertyDefinitionStatement = buildStatement()
    run( graphDB,
      removePropertyDefinitionStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[PropertyDefinition])
  }

  private def buildStatement():String = {
    val scope = buildScope(cmdScope, cmdOptions)
    return """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition {name:{pname}})
    |detach delete pd
    """.stripMargin
       .replaceAll("space", scope)
  }

  private def buildScope(datScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val scope = datScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => CommandScopes.UserSpaceScope.scope
      case CommandScopes.DataSetScope => {
        var filter:String = null
        if(cmdOptions.contains("dsId")){
          filter = "%s {mid:{dsId}}".format(CommandScopes.DataSetScope.scope)
        }else if(cmdOptions.contains("dsName")){
          filter = "%s {name:{dsName}}".format(CommandScopes.DataSetScope.scope)
        }else{
          val msg = """
          |RemoveElementPropertyDefinition requires that dsId or dsName is provided on
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
