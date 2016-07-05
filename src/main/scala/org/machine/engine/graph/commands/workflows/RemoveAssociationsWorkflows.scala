package org.machine.engine.graph.commands.workflows

import com.typesafe.scalalogging.{LazyLogging}
import org.neo4j.graphdb.GraphDatabaseService

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.exceptions.InternalErrorException
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScope, CommandScopes, FindInboundAssociationsByElementId, FindOutboundAssociationsByElementId, QueryCmdResult}
import org.machine.engine.graph.nodes.Association

object RemoveAssociationsWorkflows extends LazyLogging{
  import Neo4JHelper._

  def removeInboundAssociations(capsule: CapsuleWithContext):CapsuleWithContext = {
    val wf = Function.chain(Seq(
      requireElementId,
      findInboundAssociations,
      buildRemoveInboundAssociationsStmt,
      removeAssociations
    ))
    return wf(capsule)
  }

  def removeOutboundAssociations(capsule: CapsuleWithContext):CapsuleWithContext = {
    val wf = Function.chain(Seq(
      requireElementId,
      findOutboundAssociations,
      buildRemoveOutboundAssociationsStmt,
      removeAssociations
    ))
    return wf(capsule)
  }

  val requireElementId = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = capsule._5 == Left(WorkflowStatuses.OK)
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      val status = if(capsule._3.contains(ElementId)) Left(WorkflowStatuses.OK) else Right(ElementIdMissingErrorMsg)
      return (capsule._1, capsule._2, capsule._3, capsule._4, status)
    }
  }

  val ExistingAssociations = "ExistingAssociations"
  val findInboundAssociations = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK)
    }
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      val result:QueryCmdResult[Association] = new FindInboundAssociationsByElementId(capsule._1, capsule._2, capsule._3).execute()
      capsule._4 += (ExistingAssociations -> result.results)
      return capsule
    }
  }

  val findOutboundAssociations = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK)
    }
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      val result:QueryCmdResult[Association] = new FindOutboundAssociationsByElementId(capsule._1, capsule._2, capsule._3).execute()
      capsule._4 += (ExistingAssociations -> result.results)
      return capsule
    }
  }

  val ExistingAssociationsRequiredErrorMsg = "Internal Error: ExistingAssociations is required in the context for RemoveAssociationsWorkflows.buildRemoveInboundAssociationsStmt()."
  val AssociationIds = "associationIds"
  val RemoveAssociationsStmt = "RemoveAssociationsStmt"
  val buildRemoveInboundAssociationsStmt = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK)
    }
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      if(!capsule._4.contains(ExistingAssociations)){
        return (capsule._1, capsule._2, capsule._3, capsule._4, Right(ExistingAssociationsRequiredErrorMsg))
      }
      val ids = capsule._4(ExistingAssociations).asInstanceOf[Seq[Association]].map{ a => a.id}
      capsule._4 += (AssociationIds -> ids)
      val statement = """
      |match (x)-[association]->(y)
      |where association.associationId in {associationIds}
      | and y.mid = {elementId}
      |delete association
      """.stripMargin
      capsule._4 += (RemoveAssociationsStmt -> statement)
      return capsule
    }
  }

  val AssociationIdsRequiredErrorMsg = "Internal Error: AssociationIds is required by RemoveAssociationsWorkflows.removeAssociations()."
  val RemoveAssociationsStmtRequiredErrorMsg = "Internal Error: RemoveAssociationsStmt is required by RemoveAssociationsWorkflows.removeAssociations()."
  val removeAssociations = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK)
    }
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)){
        return capsule
      }else if(!capsule._4.contains(AssociationIds)){
        return (capsule._1, capsule._2, capsule._3, capsule._4, Right(AssociationIdsRequiredErrorMsg))
      }else if(!capsule._4.contains(RemoveAssociationsStmt)){
        return (capsule._1, capsule._2, capsule._3, capsule._4, Right(RemoveAssociationsStmtRequiredErrorMsg))
      }

      val statement = capsule._4(RemoveAssociationsStmt).toString
      val existingAssocIds = capsule._4(AssociationIds).asInstanceOf[Seq[String]]
      val options = capsule._3.toJavaMap
      options.put("associationIds", existingAssocIds)
      var status: Status = null;
      try{
        run(capsule._1,
          statement,
          options,
          emptyResultProcessor[Association])
        status = Left(WorkflowStatuses.OK)
      }catch{
        case e: Throwable => {
          logger.error("Error raised while trying to remove associations.", e)
          status = Right(e.getMessage())
        }
      }
      return (capsule._1, capsule._2, capsule._3, capsule._4, status)
    }
  }

  val buildRemoveOutboundAssociationsStmt = new PartialFunction[CapsuleWithContext, CapsuleWithContext]{
    def isDefinedAt(capsule: CapsuleWithContext):Boolean = {
      capsule._5 == Left(WorkflowStatuses.OK)
    }
    def apply(capsule: CapsuleWithContext):CapsuleWithContext = {
      if(!isDefinedAt(capsule)) return capsule
      if(!capsule._4.contains(ExistingAssociations)){
        return (capsule._1, capsule._2, capsule._3, capsule._4, Right(ExistingAssociationsRequiredErrorMsg))
      }
      val ids = capsule._4(ExistingAssociations).asInstanceOf[Seq[Association]].map{ a => a.id}
      capsule._4 += (AssociationIds -> ids)
      val statement = """
      |match (x)-[association]->(y)
      |where association.associationId in {associationIds}
      | and x.mid = {elementId}
      |delete association
      """.stripMargin
      capsule._4 += (RemoveAssociationsStmt -> statement)
      return capsule
    }
  }
}
