package org.machine.engine.graph.commands.elementdefinition

import com.typesafe.scalalogging.{LazyLogging}
import scala.collection.mutable.ArrayBuffer
import scala.util.{Either, Left, Right}
import org.machine.engine.graph.Neo4JHelper
import org.neo4j.graphdb.GraphDatabaseService
import org.machine.engine.exceptions.{InternalErrorException}
import org.machine.engine.graph.commands.{CommandScope, CommandScopes, FindElementDefinitionByName, GraphCommandOptions, QueryCmdResult}
import org.machine.engine.graph.nodes.{ElementDefinition}

object ElementDefintionWorkflowFunctions extends LazyLogging{
  import Neo4JHelper._

  private val elementDefAlreadyExistsErrorMsg = "Element Definition already exists with the provided name."

  sealed trait WorkflowStatus{
    def value:Boolean
  }

  object WorkflowStatuses{
    case object OK extends WorkflowStatus{val value = true}
    case object Error extends WorkflowStatus{val value = false}
  }

  type WorkflowErrorMsg = String
  type Status = Either[WorkflowStatus, WorkflowErrorMsg]
  type Capsule = (GraphDatabaseService,
    CommandScope,
    GraphCommandOptions,
    Status)

  val MissingMidErrorMsg            = "The command CreateElementDefinition requires the option mid."
  val MissingNameErrorMsg           = "The command CreateElementDefinition requires the option name."
  val MissingDescErrorMsg           = "The command CreateElementDefinition requires the option description."
  val MissingCreationTimeErrorMsg   = "The command CreateElementDefinition requires the option creationTime."
  val DataSetFilterRequiredErrorMsg = "For scope type DataSet, either dsId or dsName must be provided."

  val CreateElementDefintionStmt = "createElementDefinitionStmt"
  val ElementDefinitionId = "edId"
  val Mid = "mid"
  val Name = "name"
  val Description = "description"
  val CreationTime = "creationTime"
  val ElementDefinitionCreationFailureErrorMsg = "Internal Error: Element Definition could not be created."
  val DataSetId = "dsId"
  val DataSetName = "dsName"
  val CreatedElementDefinitionId = "createdElementDefinitionId"
  val Empty = ""

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

  /*
  Used after mIdGuard...
  */
  val verifyRequiredCmdOptions = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = capsule._4 == Left(WorkflowStatuses.OK)
    def apply(capsule:Capsule):Capsule = {
      if(!isDefinedAt(capsule)) return capsule
      val errorMsg:Option[String] = if (!capsule._3.contains(Mid)){
        Some(MissingMidErrorMsg)
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
      val status:Status = if(response.results.isEmpty) Left(WorkflowStatuses.OK) else Right(elementDefAlreadyExistsErrorMsg)
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

  private def generateScopeFilter(cmdScope: CommandScope, options: GraphCommandOptions):String = {
    val filter = cmdScope match {
      case CommandScopes.SystemSpaceScope => Empty
      case CommandScopes.UserSpaceScope => Empty /*TODO Make User Space an actual User*/
      case CommandScopes.DataSetScope => {
        val dsFilter = if(options.contains(DataSetId)){
          "where ss.mid = {dsId}"
        }else if(options.contains(DataSetName)){
          "where ss.name = {dsName}"
        }else{
          throw new InternalErrorException(DataSetFilterRequiredErrorMsg)
        }
        dsFilter
      }
      case _ => throw new InternalErrorException("No Matching Scope Found: "+cmdScope)
    }
    return filter
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

  val ElementDefinitionStatusCouldNotBeCreatedErrorMsg = "Element Definition could not be created."
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
      }
      Right(ElementDefinitionStatusCouldNotBeCreatedErrorMsg)
    }

  // createPropertyDefinitions,
  // processResponse
}
