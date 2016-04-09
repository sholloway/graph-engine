package org.machine.engine.graph.commands

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}


import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

/** Find an ElementDefinition in a specified graph space by ID or Name.
*/
trait FindElementDefinition extends Neo4JQueryCommand[ElementDefinition]{
  protected def buildScope(cmdScope: CommandScope,
    cmdOptions: GraphCommandOptions):String = {
    val scope = cmdScope match{
      case CommandScopes.SystemSpaceScope => CommandScopes.SystemSpaceScope.scope
      case CommandScopes.UserSpaceScope => CommandScopes.UserSpaceScope.scope
      case CommandScopes.DataSetScope => {
        var str:String = null
        if(cmdOptions.contains("dsId")){
          str = "%s {mid:{dsId}}".format(CommandScopes.DataSetScope.scope)
        }else if(cmdOptions.contains("dsName")){
          str = "%s {name:{dsName}}".format(CommandScopes.DataSetScope.scope)
        }
        str
      }
    }
    return scope
  }

  protected def getDataSetIdentifier(cmdOptions: GraphCommandOptions):String = {
    var dsIdentifier:String = null
    if(cmdOptions.contains("dsId")){
      dsIdentifier = cmdOptions.option[String]("dsId")
    }else if(cmdOptions.contains("dsName")){
      dsIdentifier = cmdOptions.option[String]("dsName")
    }
    return dsIdentifier
  }

  protected def elementDefAndPropDefQueryMapper(
    results: ArrayBuffer[(org.machine.engine.graph.nodes.ElementDefinition, PropertyDefinition)],
    record: java.util.Map[java.lang.String, Object]) = {
    val ed = mapElementDefintion(record)
    val pd = mapPropertyDefintion(record)
    val pair = (ed, pd)
    results += pair
  }

  protected def consolidateElementDefs(records: List[(ElementDefinition, PropertyDefinition)]):List[ElementDefinition] ={
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

  protected def mapElementDefintion(record: java.util.Map[java.lang.String, Object]):ElementDefinition = {
    val elementId = record.get("elementId").toString()
    val elementName = record.get("elementName").toString()
    val elementDescription = record.get("elementDescription").toString()
    return new ElementDefinition(elementId, elementName, elementDescription)
  }

  protected def mapPropertyDefintion(record: java.util.Map[java.lang.String, Object]):PropertyDefinition = {
    val propId = record.get("propId").toString()
    val propName = record.get("propName").toString()
    val propType = record.get("propType").toString()
    val propDescription = record.get("propDescription").toString()
    return new PropertyDefinition(propId, propName, propType, propDescription)
  }
}
