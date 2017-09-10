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

class EditElementDefintion(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4UpdateCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():UpdateCmdResult[String] = {
    logger.debug("EditElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      editElementDefinition(graphDB)
    })
    return UpdateCmdResult(cmdOptions.option[String]("mid"))
  }

  private def editElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("EditElementDefintion: Editing element definition.")
    val prefix = "ed"
    val exclude = List("mid", "dsId", "dsName")
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
      case CommandScopes.UserSpaceScope => {
        val filter:String = if (cmdOptions.contains(UserId)){
          s"${CommandScopes.UserSpaceScope.scope} {mid:{${UserId}}}"
        }else{
          throw new InternalErrorException(UserSpaceFilterRequiredErrorMsg)
        }
        filter
      }
      case CommandScopes.DataSetScope => {
        var filter:String = if(cmdOptions.contains(DataSetId)){
          s"${CommandScopes.DataSetScope.scope} {mid:{${DataSetId}}}"
        }else if(cmdOptions.contains(DataSetName)){
          s"${CommandScopes.DataSetScope.scope} {name:{${DataSetName}}}"
        }else{
          val msg = """
          |EditElementDefintion requires that dsId or dsName is provided on
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
