package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

import org.machine.engine.graph.commands.workflows.ListAllElementsWorkflow

class ListAllElements(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[Element] with LazyLogging{
  import org.machine.engine.graph.commands.workflows._

  def execute():QueryCmdResult[Element] = {
    logger.debug("ListAllElements: Executing Command")
    val wfResult = ListAllElementsWorkflow.workflow((database,
      cmdScope,
      cmdOptions,
      mutable.Map.empty[String, Any],
      Left(WorkflowStatuses.OK)))

    var result:QueryCmdResult[Element] = null
    wfResult._5 match {
      case Left(status) => {
        val elementsOpt = wfResult._4.get("Elements")
        val elements:List[Element] = elementsOpt.getOrElse(List.empty[Element]).asInstanceOf[List[Element]]
        result = QueryCmdResult[Element](elements)
      }
      case Right(errorMsg) => {
        result = QueryCmdResult[Element](Seq.empty[Element], EngineCmdResultStatuses.Error, Some(errorMsg))
      }
    }
    return result
  }
}
