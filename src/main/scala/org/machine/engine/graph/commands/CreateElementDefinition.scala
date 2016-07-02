package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import scala.util.{Either, Left, Right}
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

import org.machine.engine.graph.commands.elementdefinition.ElementDefintionWorkflowFunctions

class CreateElementDefintion(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import ElementDefintionWorkflowFunctions._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateElementDefintion: Executing Command")

    val wfResult = ElementDefintionWorkflowFunctions.workflow((database,
      cmdScope,
      cmdOptions,
      Left(WorkflowStatuses.OK)))    

    var result:InsertCmdResult[String] = null
    wfResult._4 match {
      case Left(status) => { //Right now status will always be OK.
        val edId = wfResult._3.option[String](CreatedElementDefinitionId)
        result = InsertCmdResult[String](edId)
      }
      case Right(errorMsg) => {
        result = InsertCmdResult[String](Empty, EngineCmdResultStatuses.Error, Some(errorMsg))
      }
    }
    return result
  }
}
