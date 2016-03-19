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

class EditElementDefintion(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("EditElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editElementDefinition(graphDB)
    })
    return cmdOptions.option[String]("mid")
  }

  private def editElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementDefintion: Editing element definition.")
    val prefix = "ed"
    val exclude = List("mid")
    val setClause = buildSetClause(prefix, cmdOptions.keys, exclude)
    val scope = buildScope(cmdScope, cmdOptions)

    val editElementDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |set setClause, ed.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", scope)
       .replaceAll("setClause", setClause)

    run( graphDB,
      editElementDefinitionStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[ElementDefinition])
  }

  private def buildScope(cmdScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val scope = cmdScope match{
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
          |EditElementDefintion requires that dsId or dsName is provided on
          |cmdOptions when the scope is of type CommandScopes.DataSet.
          """.stripMargin
          throw new InternalErrorException(msg)
        }
        filter
      }
    }
    return scope
  }
}
