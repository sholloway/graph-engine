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

class FindElementById(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JQueryCommand[Element]{
  import Neo4JHelper._

  private var elementDefintion:scala.collection.immutable.Map[String, AnyRef] = null

  /*
  This is going to be a trick. I don't know what the keys are to query.
  Possible solutions:
  - Make two queries. First find all keys and labels on the node.
    match (n {mid:{mid}}) return keys(u), labels(u)
  */
  def execute():List[Element] = {
    logger.debug("FindElementById: Executing Command")
    val definitions = findElementStructure(database, commandOptions)
    elementDefintion = definitions(0)
    val elements = findElements(database, commandOptions, elementDefintion)
    return validateElement(elements, commandOptions)
  }

  private def findElements(database:GraphDatabaseService,
    commandOptions: Map[String, AnyRef],
    elementDefintion: scala.collection.immutable.Map[String, AnyRef]):List[Element] = {
    val statement = buildFindElementQuery(commandOptions, elementDefintion);
    val elements = query[Element](database, statement, commandOptions, elementMapper)
    return elements.toList
  }

  private def validateElement(elements: List[Element], commandOptions:Map[String, AnyRef]):List[Element] = {
    val mid = commandOptions.get("mid").getOrElse(throw new InternalErrorException("FindElementById requires that mid be specified on commandOptions."))
    if(elements.length < 1){
      val msg = "No element with mid: %s could be found.".format(mid)
      throw new InternalErrorException(msg);
    }else if(elements.length > 1){
      val msg = "Multiple element where found with mid: %s".format(mid)
      throw new InternalErrorException(msg);
    }
    return elements
  }

  /*
  NOTE:
  To get started, just assume that every field is a String. Eventually, this
  is going to have to look at the corrisponding ElementDefinition and find the
  field type and do intelligent provisioning using reflection.

  I'm going to have to give this some thought.

  We know there are special cases:
  - creation_time and last_edit_time should not be Strings.
  */
  private def elementMapper(results: ArrayBuffer[Element],
    record: java.util.Map[java.lang.String, Object]) = {
      val specialFields = List("mid", "element_description", "creation_time", "last_modified_time")
      val labels:List[String] = elementDefintion.get("labels").get.asInstanceOf[List[String]]
      val fields:List[String] = elementDefintion.get("keys").get.asInstanceOf[List[String]]
      val elementType = labels(0)

      //first get the known fields
      val mid = mapString("mid", record, true)
      val elementDescription = mapString("element_description", record, true)
      val creationTime = mapString("creation_time",record, true)
      val lastEditTime = mapString("last_modified_time", record, false)

      //then get the unknown fields
      val mappedFields:Map[String, AnyRef] = Map()
      fields.foreach(field => {
        if (!specialFields.contains(field)){
          val temp = mapString(field, record, true)
          mappedFields.+=(field -> temp)
        }
      })

      val element = new Element(mid, elementType, elementDescription,
        mappedFields.toMap, creationTime, lastEditTime)
      results += element
  }

  /*
  TODO:
  Make this specific to a data set.
  match(ds:internal_data_set {mid:{dsId}})
  match (n {mid:{mid}})
  match (ds)-[:contains]->(n)
  */
  private def findElementStructure(database:GraphDatabaseService,
    commandOptions:Map[String, AnyRef]
  ):List[scala.collection.immutable.Map[String, AnyRef]] = {
    val statement = """
    |match (n {mid:{mid}})
    |return keys(n) as keys, labels(n) as labels
    """.stripMargin

    val records = query[scala.collection.immutable.Map[String, AnyRef]](database,
      statement, commandOptions, elementStructureMapper)
    return records.toList
  }

  private def buildFindElementQuery(commandOptions: Map[String, AnyRef],
    elementDefintion: scala.collection.immutable.Map[String, AnyRef]):String = {
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

  private def elementStructureMapper(results: ArrayBuffer[scala.collection.immutable.Map[String, AnyRef]],
    record: java.util.Map[java.lang.String, Object]) = {
      val labels:List[String] = record.get("labels").asInstanceOf[java.util.List[String]].toList
      val keys:List[String] = record.get("keys").asInstanceOf[java.util.List[String]].toList
      results += scala.collection.immutable.Map("keys"->keys, "labels"->labels)
  }
}
