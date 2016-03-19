package org.machine.engine.graph.commands

import org.neo4j.graphdb._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import org.machine.engine.logger._
import org.machine.engine.exceptions._
import org.machine.engine.graph._
import org.machine.engine.graph.commands._
import org.machine.engine.graph.nodes._
import org.machine.engine.graph.labels._
import org.machine.engine.graph.internal._

/*
This command deviates from the normal path of having cmdOptions:Map[String, AnyRef]
because a field could be of type AnyVal or AnyRef. Example: Byte & List[Byte]
*/
class FindElementById(database: GraphDatabaseService,
  cmdScope: CommandScope,
  fields: GraphCommandOptions,
  logger: Logger){
  import Neo4JHelper._

  private var elementDefintion:scala.collection.immutable.Map[String, List[String]] = null

  def execute():Element = {
    logger.debug("FindElementById: Executing Command")
    val definitions = findElementStructure(database, fields)
    elementDefintion = definitions(0)
    return findElements(database, fields, elementDefintion)
  }

  private def findElements(database: GraphDatabaseService,
    fields: GraphCommandOptions,
    elementDefintion: scala.collection.immutable.Map[String, List[String]]):Element = {
    val statement = buildFindElementQuery(fields, elementDefintion);
    val elements = query[Element](database, statement, fields.toJavaMap, elementMapper)
    return validateElement(elements.toList, fields)(0)
  }

  private def validateElement(elements: List[Element], fields: GraphCommandOptions):List[Element] = {
    val mid = fields.field[String]("mid")
    if(elements.length < 1){
      val msg = "No element with mid: %s could be found.".format(mid)
      throw new InternalErrorException(msg);
    }else if(elements.length > 1){
      val msg = "Multiple element where found with mid: %s".format(mid)
      throw new InternalErrorException(msg);
    }
    return elements
  }

  private def elementMapper(results: ArrayBuffer[Element],
    record: java.util.Map[java.lang.String, Object]) = {
      val specialFields = List("mid", "element_description", "creation_time", "last_modified_time")
      val labels:List[String] = elementDefintion.get("labels").get.asInstanceOf[List[String]]
      val properties:List[String] = elementDefintion.get("keys").get.asInstanceOf[List[String]]
      val elementType = labels(0)

      //first get the known fields
      val mid = mapString("mid", record, true)
      val elementDescription = mapString("element_description", record, true)
      val creationTime = mapString("creation_time",record, true)
      val lastEditTime = mapString("last_modified_time", record, false)

      //then get the unknown fields
      val mappedFields:Map[String, Any] = Map()
      properties.foreach(property => {
        if (!specialFields.contains(property)){
          propertyGuard(property, record)
          val value = record.get(property)
          mappedFields.+=(property -> value)
        }
      })

      val element = new Element(mid, elementType, elementDescription,
        mappedFields.toMap, creationTime, lastEditTime)
      results += element
  }

  def propertyGuard(property: String, record: java.util.Map[java.lang.String, Object]):Unit = {
    if (!record.containsKey(property)){
      val msg = "The required field: %s was not found in the query response.".format(property)
      throw new InternalErrorException(msg)
    }
  }

  /*
  TODO:
  Make this specific to a data set.
  match(ds:internal_data_set {mid:{dsId}})
  match (n {mid:{mid}})
  match (ds)-[:contains]->(n)
  */
  private def findElementStructure(database: GraphDatabaseService,
    fields: GraphCommandOptions
  ):List[scala.collection.immutable.Map[String, List[String]]] = {
    val statement = """
    |match (n {mid:{mid}})
    |return keys(n) as keys, labels(n) as labels
    """.stripMargin
    val records = query[scala.collection.immutable.Map[String, List[String]]](database,
      statement,
      fields.toJavaMap,
      elementStructureMapper)
      logger.debug("FindElementById: findElementStructure returned records:")
      logger.debug(records.toString)
    return records.toList
  }

  private def buildFindElementQuery(fields: GraphCommandOptions,
    elementDefintion: scala.collection.immutable.Map[String, List[String]]):String = {
    var prefix = "e"
    val keys:List[String] = elementDefintion.get("keys").get.asInstanceOf[List[String]]
    val fetchClause = buildFetchClause(prefix, keys)
    val labels:List[String] = elementDefintion.get("labels").get.asInstanceOf[List[String]]
    val template = """
    |match (e:label {mid:{mid}})
    |return fetch
    """.stripMargin
      .replaceAll("fetch", fetchClause)
      .replaceAll("label", labels(0))
    logger.debug("FindElementById: buildFindElementQuery")
    logger.debug(template)
    return template
  }

  private def elementStructureMapper(results: ArrayBuffer[scala.collection.immutable.Map[String, List[String]]],
    record: java.util.Map[java.lang.String, Object]) = {
      val labels:List[String] = record.get("labels").asInstanceOf[java.util.List[String]].toList
      val keys:List[String] = record.get("keys").asInstanceOf[java.util.List[String]].toList
      results += scala.collection.immutable.Map("keys"->keys, "labels"->labels)
  }
}
