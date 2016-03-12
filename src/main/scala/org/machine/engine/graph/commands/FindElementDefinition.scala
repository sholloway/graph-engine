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

/** Find an ElementDefinition in a specified graph space by ID or Name.
*/
trait FindElementDefinition extends Neo4JQueryCommand[ElementDefinition]{
  import Neo4JHelper._

  def execute():List[ElementDefinition] = {
    logger.debug("FindElementDefinition: Executing Command")
    val findElement = buildQuery(cmdScope, commandOptions)
    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findElement,
      commandOptions,
      elementDefAndPropDefQueryMapper)
    val elementDefs = consolidateElementDefs(records.toList)
    return validateQueryResponse(elementDefs);
    return null;
  }

  protected def buildQuery(cmdScope:CommandScope, commandOptions:Map[String, AnyRef]):String = {
    val edMatchClause = buildElementDefinitionMatchClause(commandOptions)
    return """
      |match (ss:scope)-[:exists_in]->(ed:element_definition ed_match)-[:composed_of]->(pd:property_definition)
      |return ed.mid as elementId,
      |  ed.name as elementName,
      |  ed.description as elementDescription,
      |  pd.mid as propId,
      |  pd.name as propName,
      |  pd.type as propType,
      |  pd.description as propDescription
      """.stripMargin
        .replaceAll("scope", cmdScope.scope)
        .replaceAll("ed_match", edMatchClause)
  }

  protected def commandOptions:Map[String, AnyRef]
  protected def database:GraphDatabaseService
  protected def cmdScope:CommandScope
  protected def logger:Logger

  protected def buildElementDefinitionMatchClause(commandOptions:Map[String, AnyRef]):String
  protected def validateQueryResponse(elementDefs: List[ElementDefinition]):List[ElementDefinition]

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
