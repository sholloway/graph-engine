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

class ListAllElementDefintions(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger){
  import Neo4JHelper._

  /*
  How do I want this to work? A query needs to return back a list of items.
  Transactions do not.

  There is going to be two levels of commnds.
  0MQ -> Engine Protocol (Rules) Map -> Engine Command -> DSL -> GraphCommand

  */
  def execute():List[ElementDefinition] = {
    logger.debug("CreateElementDefintion: Executing Command")
    //TODO: Currently this only returns ElementDefinitions that have associated PropertyDefinitions.
    //TODO: Return creation_time & last_modified_time
    val findDefinedElements = """
      |match (ss:internal_system_space)-[:exists_in]->(ed:element_definition)-[:composed_of]->(pd:property_definition)
      |return ed.mid as elementId,
      |  ed.name as elementName,
      |  pd.mid as propId,
      |  pd.name as propName,
      |  pd.type as propType,
      |  pd.description as propDescription
      """.stripMargin

    val records = query[(ElementDefinition, PropertyDefinition)](database,
      findDefinedElements, null,
      elementDefAndPropDefQueryMapper)
    return consolidateElementDefs(records.toList)
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
