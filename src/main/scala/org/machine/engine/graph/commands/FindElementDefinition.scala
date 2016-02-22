package org.machine.engine.graph.commands

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.logger._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

class FindElementDefinition(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger){
  import Neo4JHelper._

  def execute():ElementDefinition = {
    logger.debug("FindElementDefinition: Executing Command")
    //TODO: Currently this only returns ElementDefinitions that have associated PropertyDefinitions.
    //TODO: Return creation_time & last_modified_time
    val findElement = """
      |match (ss:scope)-[:exists_in]->(ed:element_definition {mid:{mid}})-[:composed_of]->(pd:property_definition)
      |return ed.mid as elementId,
      |  ed.name as elementName,
      |  pd.mid as propId,
      |  pd.name as propName,
      |  pd.type as propType,
      |  pd.description as propDescription
      """.stripMargin.replaceAll("scope", cmdScope.scope)

    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement, commandOptions,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)

    if(elementDefs.length < 0){
      throw new InternalErrorException("No element with ID: %s could be found in %".format(commandOptions.get("mid"), cmdScope.scope));
    }else if(elementDefs.length > 1){
      throw new InternalErrorException("Multiple Element Definitions where found with ID: %s could be found in %".format(commandOptions.get("mid"), cmdScope.scope));
    }
    return elementDefs(0);
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
    return new ElementDefinition(elementId, elementName)
  }

  private def mapPropertyDefintion(record: java.util.Map[java.lang.String, Object]):PropertyDefinition = {
    val propId = record.get("propId").toString()
    val propName = record.get("propName").toString()
    val propType = record.get("propType").toString()
    val propDescription = record.get("propDescription").toString()
    return new PropertyDefinition(propId, propName, propType, propDescription)
  }
}
