package org.machine.engine.graph.commands

import com.typesafe.scalalogging._
import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class ListAllElementDefinitions(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4JQueryCommand[ElementDefinition] with LazyLogging{
  import Neo4JHelper._

  def execute():QueryCmdResult[ElementDefinition] = {
    logger.debug("ListAllElementDefintions: Executing Command")
    val scope = buildScope(cmdScope, cmdOptions)
    //#TODO:10 Currently this only returns ElementDefinitions that have associated PropertyDefinitions.
    //#TODO:80 Return creation_time & last_modified_time
    val findDefinedElements = """
      |match (ss:space)-[:exists_in]->(ed:element_definition)-[:composed_of]->(pd:property_definition)
      |return ed.mid as elementId,
      |  ed.name as elementName,
      |  ed.description as elementDescription,
      |  pd.mid as propId,
      |  pd.name as propName,
      |  pd.type as propType,
      |  pd.description as propDescription
      """.stripMargin
        .replaceAll("space", scope)

    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findDefinedElements, cmdOptions.toJavaMap,
      elementDefAndPropDefQueryMapper)
    return QueryCmdResult(consolidateElementDefs(records.toList))
  }

  protected def buildScope(cmdScope: CommandScope, cmdOptions: GraphCommandOptions):String = {
    val scope = cmdScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => {
        s"${CommandScopes.UserSpaceScope.scope} {mid:{activeUserId}}"
      }
      case CommandScopes.DataSetScope => {
        val str:String = if(cmdOptions.contains("dsId")){
          s"${CommandScopes.DataSetScope.scope} {mid:{dsId}}"
        }else if(cmdOptions.contains("dsName")){
          s"${CommandScopes.DataSetScope.scope} {name:{dsName}}"
        }else{
          throw new InternalErrorException("For scope type of DataSetScope, the option dsId or dsName must be provided.")
        }
        str
      }
    }
    return scope
  }

  private def elementDefAndPropDefQueryMapper(
    results: ArrayBuffer[(ElementDefinition, PropertyDefinition)],
    record: java.util.Map[java.lang.String, Object]) = {
    val ed = mapElementDefintion(record)
    val pd = mapPropertyDefintion(record)
    val pair = (ed, pd)
    results += pair
  }

  private def consolidateElementDefs(records: List[(ElementDefinition, PropertyDefinition)]):List[ElementDefinition] ={
    val elementsMap = Map[String, ElementDefinition]()
    var ed:ElementDefinition = null;
    var pd:PropertyDefinition = null;
    records.foreach(r => {
      ed = r._1
      pd = r._2
      if(elementsMap.contains(ed.id)){
        elementsMap.get(ed.id).get.addProperty(pd)
      }else{
        ed.addProperty(pd)
        elementsMap += (ed.id -> ed)
      }
    })
    return elementsMap.values.toList
  }

  private def mapElementDefintion(record: java.util.Map[java.lang.String, Object]):ElementDefinition = {
    val elementId = record.get("elementId").toString()
    val elementName = record.get("elementName").toString()
    val elementDescription = record.get("elementDescription").toString()
    return new ElementDefinition(elementId, elementName, elementDescription)
  }

  private def mapPropertyDefintion(record: java.util.Map[java.lang.String, Object]):PropertyDefinition = {
    val propId = record.get("propId").toString()
    val propName = record.get("propName").toString()
    val propType = record.get("propType").toString()
    val propDescription = record.get("propDescription").toString()
    return new PropertyDefinition(propId, propName, propType, propDescription)
  }
}
