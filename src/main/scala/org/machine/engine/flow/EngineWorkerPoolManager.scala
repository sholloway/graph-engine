package org.machine.engine.flow

import org.machine.engine.Engine
import org.machine.engine.flow.requests.RequestMessage
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, EngineCmdResultStatuses, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult, GraphCommandOptions}
import org.machine.engine.graph.decisions.{ActionTypes, EntityTypes, Filters}
import org.machine.engine.graph.nodes._
import org.machine.engine.encoder.json.OutboundJSONSerializer

object EngineWorkerPoolManager{
  type Capsule = (RequestMessage, GraphCommandOptions)

  def execute(capsule: EngineCapsule):EngineMessage = {
    val request = capsule.attributes("deserializedMsg").asInstanceOf[RequestMessage]
    val options = buildOptions(request)
    val result:EngineCmdResult = Engine.getInstance
      .reset
      .setUser(Some(request.userId))
      .setScope(CommandScopes.pickScope(request.scope))
      .setActionType(ActionTypes.pickAction(request.actionType))
      .setEntityType(EntityTypes.pickEntity(request.entityType))
      .setFilter(Filters.pickFilter(request.filter))
      .setOptions(options)
    .run

    val response = result.status match{
      case EngineCmdResultStatuses.OK => {
        val responseStr = OutboundJSONSerializer.serialize(result)
        new EngineMessageBase(
          capsule.id,
          EngineCapsuleStatuses.Ok.name,
          EngineMessageTypes.CmdResult.name,
          responseStr
        )
      }
      case EngineCmdResultStatuses.Error => {
        val responseStr = result.errorMessage.get
        new EngineMessageBase(
          capsule.id,
          EngineCapsuleStatuses.Error.name,
          EngineMessageTypes.CmdResult.name,
          responseStr
        )
      }
    }
    return response
  }

  private def buildOptions(request: RequestMessage):GraphCommandOptions = {
    val options = new GraphCommandOptions()
    Function.chain(Seq(
      fetchGenericOptions,
      fetchProperties
    ))((request, options))
    return options
  }

  private val fetchGenericOptions = new PartialFunction[Capsule, Capsule]{
    def isDefinedAt(capsule: Capsule):Boolean = true
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        val excluded = Seq("properties")
        capsule._1.options.foreach(field => {
          if (!excluded.contains(field._1)){
            capsule._2.addOption(field._1, field._2)
          }
        })
      }
      return capsule
    }
  }

  /*
  NOTE This is very ugly.
  */
  private val fetchProperties = new PartialFunction[Capsule, Capsule]{
    private val property = "properties"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption("properties", new PropertyDefinitions())
        val propertyDefs = capsule._1.options(property)
        if (!propertyDefs.isInstanceOf[List[_]]){
          return capsule
        }
        propertyDefs.asInstanceOf[List[Map[String, Any]]].foreach{ prop =>
          val propId = Neo4JHelper.uuid
          val pname:String = prop.getOrElse("name", "empty").toString
          val ptype:String = prop.getOrElse("propertyType", "empty").toString
          val pdesc:String = prop.getOrElse("description", "").toString
          val propDef = new PropertyDefinition(propId, pname, ptype, pdesc)
          val props = capsule._2.option[PropertyDefinitions]("properties")
          props.addProperty(propDef)
        }
      }
      return capsule
    }
  }

}
