package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, Map}

import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.commands.workflows._
import org.machine.engine.graph.internal._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.nodes._

class DeleteElementDefintion(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4DeleteCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():DeleteCmdResult[String] = {
    logger.debug("DeleteElementDefintion: Executing Command")
    transaction(database, (graphDB: GraphDatabaseService) => {
      deleteAssociatedPropertyDefinitions(graphDB)
      deleteElementDefinition(graphDB)
    })
    return DeleteCmdResult(cmdOptions.option[String]("mid"))
  }

  private def deleteAssociatedPropertyDefinitions(graphDB: GraphDatabaseService):Unit = {
    logger.debug("DeleteElementDefintion: Deleting associated property definitions")
    val scope = buildScope(cmdScope, cmdOptions)
    val statement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition)
    |detach delete pd
    """.stripMargin
       .replaceAll("space", scope)

    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[ElementDefinition])
  }

  private def deleteElementDefinition(graphDB: GraphDatabaseService):Unit = {
    logger.debug("DeleteElementDefintion: Deleting element definition.")
    val scope = buildScope(cmdScope, cmdOptions)
    val statement = """
    |match (ss:space)-[:exists_in]->(ed:element_definition {mid:{mid}})
    |detach delete ed
    """.stripMargin
       .replaceAll("space", scope)

    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[ElementDefinition])
  }

  protected def buildScope(cmdScope: CommandScope, options: GraphCommandOptions):String = {
    val scope = cmdScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => {
        val filter:String = if (options.contains(UserId)){
          s"${CommandScopes.UserSpaceScope.scope} {mid:{${UserId}}}"
        }else{
          throw new InternalErrorException(UserSpaceFilterRequiredErrorMsg)
        }
        filter
      }
      case CommandScopes.DataSetScope => {
        val str:String = if(options.contains("dsId")){
          s"${CommandScopes.DataSetScope.scope} {mid:{${DataSetId}}}"
        }else if(options.contains("dsName")){
          s"${CommandScopes.DataSetScope.scope} {name:{${DataSetName}}}"
        }else{
          val msg = """
          |DeleteElementDefintion requires that dsId or dsName is provided on
          |cmdOptions when the scope is of type CommandScopes.DataSet.
          """.stripMargin
          throw new InternalErrorException(msg)
        }
        str
      }
      case _ => throw new InternalErrorException(s"No Matching Scope Found: ${cmdScope}")
    }
    return scope
  }
}
