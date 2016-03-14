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

  def execute():String = {
    logger.debug("EditElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editElementDefinition(graphDB)
    })
    val mid = commandOptions.get("mid").getOrElse(throw new InternalErrorException("mid required"))
    return mid.toString
  }

  private def editElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementDefintion: Editing element definition.")
    val setClause = buidSetClause(commandOptions)
    val scope = buildScope(cmdScope, commandOptions)

    val editElementDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |set setClause, ed.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", scope)
       .replaceAll("setClause", setClause)

    run( graphDB,
      editElementDefinitionStatement,
      commandOptions,
      emptyResultProcessor[ElementDefinition])
  }

  private def buildScope(cmdScope:CommandScope, options:Map[String, AnyRef]):String = {
    val scope = cmdScope match{
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
          |EditElementDefintion requires that dsId or dsName is provided on
          |commandOptions when the scope is of type CommandScopes.DataSet.
          """.stripMargin
          throw new InternalErrorException(msg)
        }
        filter
      }
    }
    return scope
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
