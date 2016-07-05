package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._
import org.machine.engine.graph.commands.workflows.RemoveAssociationsWorkflows

class RemoveInboundAssociations(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends InternalEngineCommand with LazyLogging{
  import org.machine.engine.graph.commands.workflows._
  import RemoveAssociationsWorkflows._

  def execute():DeleteSetCmdResult = {
    logger.debug("RemoveInboundAssociations: Executing Command")
    val wfResult = removeInboundAssociations((database,
      cmdScope,
      cmdOptions,
      mutable.Map.empty[String, Any],
      Left(WorkflowStatuses.OK)
    ))
    return wfResult._5 match {
      case Left(status)    => DeleteSetCmdResult()
      case Right(errorMsg) => DeleteSetCmdResult(EngineCmdResultStatuses.Error, Some(errorMsg.toString))
    }
  }
}
