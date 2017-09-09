package org.machine.engine.graph.commands.workflows

import com.typesafe.scalalogging.{LazyLogging}
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.util.{Either, Left, Right}
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.exceptions.{InternalErrorException}
import org.machine.engine.graph.commands.{CommandScope, CommandScopes, FindElementDefinitionByName, GraphCommandOptions, QueryCmdResult}
import org.machine.engine.graph.nodes.{ElementDefinition, PropertyDefinition, PropertyDefinitions}
import org.neo4j.graphdb.GraphDatabaseService

object ElementDefintionWorkflowFunctions extends LazyLogging{
  import Neo4JHelper._
  private val ElementDefinitionStatusCouldNotBeCreatedErrorMsg = "Element Definition could not be created."

  def workflow(capsule: Capsule):Capsule = {
    val wf = Function.chain(Seq(
      mIdGuard,
      createTimeGuard,
      verifyRequiredCmdOptions,
      verifyUniqueness,
      createElementDefinitionStmt,
      createElementDefinition,
      createPropertyDefinitions))
    return wf(capsule)
  }

  val mIdGuard = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = {
      return capsule._4 == Left(WorkflowStatuses.OK) && !capsule._3.contains(Mid)
    }

    def apply(capsule: Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      capsule._3.addOption(Mid, uuid)
      return capsule
    }
  }

  val createTimeGuard = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = {
      return capsule._4 == Left(WorkflowStatuses.OK) && !capsule._3.contains(CreationTime)
    }

    def apply(capsule: Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      capsule._3.addOption(CreationTime, time)
      return capsule
    }
  }

  val verifyRequiredCmdOptions = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = capsule._4 == Left(WorkflowStatuses.OK)
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      val errorMsg:Option[String] = if (!capsule._3.contains(Mid)){
        Some(MissingMidErrorMsg)
      }else if(!capsule._3.contains(UserId)){
        Some(MissingUserIdErrorMsg)
      }else if (!capsule._3.contains(Name)){
        Some(MissingNameErrorMsg)
      }else if (!capsule._3.contains(Description)){
        Some(MissingDescErrorMsg)
      }else if (!capsule._3.contains(CreationTime)){
        Some(MissingCreationTimeErrorMsg)
      }else{
        None
      }
      val status:Status = if(errorMsg.isEmpty) Left(WorkflowStatuses.OK) else Right(errorMsg.get)
      return (capsule._1, capsule._2, capsule._3, status)
    }
  }

  /*
  Leverages FindElementDefinitionByName to verify if the element definition
  already exists in the provided scope.

  Requirements:
  GraphCommandOptions must contain dsId if the Scope is DataSet.

  Note
  FindElementDefinitionByName creates it's own transaction, so the workflow cannot
  be nested in a transaction.
  */
  val verifyUniqueness = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = capsule._4 == Left(WorkflowStatuses.OK)
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      val cmd = new FindElementDefinitionByName(capsule._1, capsule._2, capsule._3)
      val response:QueryCmdResult[ElementDefinition] = cmd.execute()
      val status:Status = if(response.results.isEmpty) Left(WorkflowStatuses.OK) else Right(ElementDefAlreadyExistsErrorMsg)
      return (capsule._1, capsule._2, capsule._3, status)
    }
  }

  /*
  Find the appropriate scope, then create a new element definition in it.
  If nothing is returned or an exception is thrown set the status to an error message
  to prevent orphaned Property Defs from being created.
  */
  val createElementDefinitionStmt = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = capsule._4 == Left(WorkflowStatuses.OK)
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      var status:Status = null
      try{
        val scopeFilter = generateScopeFilter(capsule._2, capsule._3)
        val stmt:String = """
        match (ss:scope) filter
        create (ss)-[:exists_in]->(ed:element_definition {
          mid:{mid},
          name:{name},
          description:{description},
          creation_time:{creationTime}
        })
        return ed.mid as edId
        """.stripMargin
        .replaceAll("scope", capsule._2.scope)
        .replaceAll("filter", scopeFilter)
        capsule._3.addOption(CreateElementDefintionStmt, stmt)
        status = Left(WorkflowStatuses.OK)
      }catch{
        case e:InternalErrorException => status = Right(e.getMessage())
      }
      return (capsule._1, capsule._2, capsule._3, status)
    }
  }

  val createElementDefinition = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = {
      capsule._4 == Left(WorkflowStatuses.OK) && capsule._3.contains(CreateElementDefintionStmt)
    }
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      var createdEdIds:Option[List[String]] = None
      var status:Status = null
      try{
        transaction(capsule._1, (graphDB: GraphDatabaseService) => {
          val stmt = capsule._3.option[String](CreateElementDefintionStmt)
          val ids = run[String](graphDB,
            stmt,
            capsule._3.toJavaMap,
            elementDefIdResultsProcessor)
          if(!ids.isEmpty){
            createdEdIds = Some(ids.toList)
          }
        })
        status = determineCreateElementDefinitionStatus(createdEdIds, capsule)
      }catch{
        case e: Throwable => {
          logger.error("Could not create element definition.", e)
          status = Right(ElementDefinitionCreationFailureErrorMsg)
        }
      }
      return (capsule._1, capsule._2, capsule._3, status)
    }
  }

  def elementDefIdResultsProcessor(results: ArrayBuffer[String], record: java.util.Map[java.lang.String, Object]):ArrayBuffer[String] = {
    val id = record.get(ElementDefinitionId).toString
    results += id
  }

  private def determineCreateElementDefinitionStatus(createdEdIds: Option[List[String]],
    capsule: Capsule):Status =
    if(createdEdIds.isDefined && createdEdIds.get.length == 1){
      capsule._3.addOption(CreatedElementDefinitionId, createdEdIds.get.head)
      Left(WorkflowStatuses.OK)
    }else{
      if(createdEdIds.isDefined && createdEdIds.get.length > 1){
        logger.error("Created more than one element definition.")
      }else if(createdEdIds.isDefined && createdEdIds.get.isEmpty){
        logger.error("Statement ran, but no element definitions were created.")
      }else{
        logger.error("Statement ran, but no element definitions were created.")
      }
      Right(ElementDefinitionStatusCouldNotBeCreatedErrorMsg)
    }

  val createPropertyDefinitions = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = {
      capsule._4 == Left(WorkflowStatuses.OK) &&
      capsule._3.contains(CreatedElementDefinitionId) &&
      capsule._3.contains(Properties) &&
      !capsule._3.option[PropertyDefinitions](Properties).isEmpty
    }
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      var status:Status = null
      val edId = capsule._3.option[String](CreatedElementDefinitionId)
      val createPropertyStatement = """
      |match(ed:element_definition) where ed.mid = {edId}
      |create (ed)-[:composed_of]->(pd:property_definition {
      |  mid:{mid},
      |  name:{name},
      |  type:{type},
      |  description:{description}
      |})
      """.stripMargin
      try{
        transaction(capsule._1, (graphDB: GraphDatabaseService) => {
          capsule._3.option[PropertyDefinitions](Properties).toList.foreach(property => {
            val options = property.toMap + ("edId"->edId)
            run(graphDB,
              createPropertyStatement,
              options,
              emptyResultProcessor[ElementDefinition])
          })
        })
        status = Left(WorkflowStatuses.OK)
      }catch{
        case e: Throwable => {
          logger.error("Could not create property definition.", e)
          status = Right(PropertyDefinitionCreationFailureErrorMsg)
        }
      }
      return (capsule._1, capsule._2, capsule._3, status)
    }
  }
}
