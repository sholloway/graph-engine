package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.commands.workflows._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class EditElementPropertyDefinition(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4UpdateCommand[String] with LazyLogging{
  import Neo4JHelper._

  val filter = List("mid", "pname")

  def execute():UpdateCmdResult[String] = {
    logger.debug("EditElementPropertyDefinition: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      editPropertyDefinition(graphDB)
    })
    return UpdateCmdResult(cmdOptions.option[String]("mid"))
  }

  private def editPropertyDefinition(graphDB: GraphDatabaseService):Unit = {
    logger.debug("EditElementPropertyDefinition: Editing property definition.")
    val editPropertyDefinitionStatement = buildStatement()
    run( graphDB,
      editPropertyDefinitionStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[PropertyDefinition])
  }

  private def buildStatement():String = {
    val setClause = buildSetClause("pd", cmdOptions.keys, filter)
    val scope = buildScope(cmdScope, cmdOptions)
    val editPropertyDefinitionStatement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition {name:{pname}})
    |set setClause, pd.last_modified_time = timestamp()
    """.stripMargin
       .replaceAll("space", scope)
       .replaceAll("setClause", setClause)
     return editPropertyDefinitionStatement
  }

  private def buildScope(datScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val scope = datScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => {
        val filter:String = if (cmdOptions.contains(UserId)){
          s"${CommandScopes.UserSpaceScope.scope} {mid:{${UserId}}}"
        }else{
          throw new InternalErrorException(UserSpaceFilterRequiredErrorMsg)
        }
        filter
      }
      case CommandScopes.DataSetScope => {
        val filter:String = if(cmdOptions.contains(DataSetId)){
          s"${CommandScopes.DataSetScope.scope} {mid:{${DataSetId}}}"
        }else if(cmdOptions.contains(DataSetName)){
          s"${CommandScopes.DataSetScope.scope} {name:{${DataSetName}}}"
        }else{
          val msg = """
          |EditElementPropertyDefinition requires that dsId or dsName is provided on
          |cmdOptions when the scope is of type CommandScopes.DataSet.
          """.stripMargin
          throw new InternalErrorException(msg)
        }
        filter
      }
      case _ => throw new InternalErrorException(s"No Matching Scope Found: ${cmdScope}")
    }
    return scope
  }
}
