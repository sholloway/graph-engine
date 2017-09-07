package org.machine.engine.graph.commands

package object workflows{
  import scala.collection.mutable
  import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
  import org.neo4j.graphdb.GraphDatabaseService
  import org.machine.engine.graph.commands.{CommandScope, GraphCommandOptions}
  import org.machine.engine.exceptions.InternalErrorException
  sealed trait WorkflowStatus{
    def value:Boolean
  }

  object WorkflowStatuses{
    case object OK extends WorkflowStatus{val value = true}
    case object Error extends WorkflowStatus{val value = false}
  }

  type WorkflowErrorMsg   = String
  type Status             = Either[WorkflowStatus, WorkflowErrorMsg]
  type CapsuleContext     = mutable.Map[String, Any]
  type Capsule            = (GraphDatabaseService, CommandScope, GraphCommandOptions, Status)
  type CapsuleWithContext = (GraphDatabaseService, CommandScope, GraphCommandOptions, CapsuleContext, Status)

  val ElementDefAlreadyExistsErrorMsg           = "Element Definition already exists with the provided name."
  val MissingMidErrorMsg                        = "The command CreateElementDefinition requires the option mid."
  val MissingNameErrorMsg                       = "The command CreateElementDefinition requires the option name."
  val MissingDescErrorMsg                       = "The command CreateElementDefinition requires the option description."
  val MissingCreationTimeErrorMsg               = "The command CreateElementDefinition requires the option creationTime."
  val DataSetFilterRequiredErrorMsg             = "For scope type DataSet, either dsId or dsName must be provided."
  val UserSpaceFilterRequiredErrorMsg           = "For scope type UserSpace, activeUserId must be provided."
  val ElementDefinitionCreationFailureErrorMsg  = "Internal Error: Element Definition could not be created."
  val PropertyDefinitionCreationFailureErrorMsg = "Internal Error: Property Definition could not be created."
  val ElementIdMissingErrorMsg                  = "The option elementId is required."
  val SystemSpaceIsNotSupportedMsg              = "System Space is not support for this opperation."

  val CreateElementDefintionStmt  = "createElementDefinitionStmt"
  val ElementDefinitionId         = "edId"
  val Mid                         = "mid"
  val Name                        = "name"
  val Description                 = "description"
  val CreationTime                = "creationTime"
  val DataSetId                   = "dsId"
  val UserId                      = "activeUserId"
  val DataSetName                 = "dsName"
  val CreatedElementDefinitionId  = "createdElementDefinitionId"
  val Empty                       = ""
  val Properties                  = "properties"
  val ElementDescriptionField     = "element_description"
  val CreationTimeField           = "creation_time"
  val LastModifiedTimeField       = "last_modified_time"
  val ElementId                   = "elementId"

  def generateScopeFilter(cmdScope: CommandScope, options: GraphCommandOptions):String = {
    val filter = cmdScope match {
      case CommandScopes.SystemSpaceScope => Empty
      case CommandScopes.UserSpaceScope => {
        val userFilter = if(options.contains(UserId)){
          "where ss.mid={activeUserId}"
        }else{
          throw new InternalErrorException(UserSpaceFilterRequiredErrorMsg)
        }
        userFilter
      }
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
      case _ => throw new InternalErrorException(s"No Matching Scope Found: ${cmdScope}")
    }
    return filter
  }
}
