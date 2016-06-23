package org.machine.engine.flow

import org.machine.engine.Engine
import org.machine.engine.flow.requests.RequestMessage
import org.machine.engine.graph.Neo4JHelper
import org.machine.engine.graph.commands.{CommandScopes, EngineCmdResult, QueryCmdResult, UpdateCmdResult, DeleteCmdResult, InsertCmdResult, GraphCommandOptions}
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
      .setUser(Some(request.user))
      .setScope(CommandScopes.pickScope(request.scope))
      .setActionType(ActionTypes.pickAction(request.actionType))
      .setEntityType(EntityTypes.pickEntity(request.entityType))
      .setFilter(Filters.pickFilter(request.filter))
      .setOptions(options)
    .run

    val responseStr = OutboundJSONSerializer.serialize(result)

    return new EngineMessageBase(
      capsule.id,
      EngineCapsuleStatuses.Ok.name,
      EngineMessageTypes.CmdResult.name,
      responseStr
    )
  }

  private def buildOptions(request: RequestMessage):GraphCommandOptions = {
    val options = new GraphCommandOptions()
    Function.chain(Seq(
      fetchMid,
      fetchDsId,
      fetchDsName,
      fetchPName,
      fetchName,
      fetchDescription,
      fetchProperties
    ))((request, options))
    return options
  }

  private def fetchStr(str: String,
    request: RequestMessage,
    options: GraphCommandOptions
  ) = {
    if (request.options.contains(str)){
      options.addOption(str, request.options(str))
    }
  }

  private val fetchDsId = new PartialFunction[Capsule, Capsule]{
    private val property = "dsId"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
      }
      return capsule
    }
  }

  private val fetchDsName = new PartialFunction[Capsule, Capsule]{
    private val property = "dsName"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
      }
      return capsule
    }
  }

  private val fetchMid = new PartialFunction[Capsule, Capsule]{
    private val property = "mid"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
      }
      return capsule
    }
  }

  private val fetchPName = new PartialFunction[Capsule, Capsule]{
    private val property = "pname"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
      }
      return capsule
    }
  }

  private val fetchName = new PartialFunction[Capsule, Capsule]{
    private val property = "name"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
      }
      return capsule
    }
  }

  private val fetchDescription = new PartialFunction[Capsule, Capsule]{
    private val property = "description"
    def isDefinedAt(capsule: Capsule):Boolean = capsule._1.options.contains(property)
    def apply(capsule: Capsule):Capsule = {
      if(isDefinedAt(capsule)){
        capsule._2.addOption(property,capsule._1.options(property))
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
