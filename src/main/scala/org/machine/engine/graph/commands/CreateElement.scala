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

class CreateElement(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("CreateElement: Executing Command")

    val elementDef = findElementDefinition(database, commandOptions)

    transaction(database, (graphDB:GraphDatabaseService) => {
      createElement(graphDB, commandOptions, elementDef)
      registerElement(graphDB, commandOptions, elementDef)
    })
    val mid = commandOptions.get("mid").getOrElse(throw new InternalErrorException("mid required"))
    return mid.toString
  }

  /*
  Find the required ElementDefinition. This could be in system space, user space or the dataset.
  */
  private def findElementDefinition(graphDB:GraphDatabaseService, commandOptions:Map[String, AnyRef]):ElementDefinition = {
    val statement = """
    |match (ed:element_definition) where ed.mid = {edId}
    |return ed.mid as elementId,
    |  ed.name as elementName,
    |  ed.description as elementDescription
    """.stripMargin

    val elementDefs = query[ElementDefinition](database,
      statement, commandOptions, mapElementDefs)
      val edId = commandOptions.get("edId").get.toString
    return validateElementDefinition(edId, elementDefs)
  }

  private def validateElementDefinition(edId:String,
    elementDefs: Array[ElementDefinition]
  ):ElementDefinition = {
    if(elementDefs.length < 1){
      val msg = "No element definition with ID: % could be found.".format(edId)
      throw new InternalErrorException(msg);
    }else if(elementDefs.length > 1){
      val msg = "More than one element definition with ID: % was found.".format(edId)
      throw new InternalErrorException(msg);
    }
    return elementDefs(0)
  }

  private def mapElementDefs(
    results: ArrayBuffer[ElementDefinition],
    record: java.util.Map[java.lang.String, Object]) = {
    results += mapElementDefintion(record)
  }

  private def mapElementDefintion(record: java.util.Map[java.lang.String, Object]):ElementDefinition = {
    val elementId = record.get("elementId").toString()
    val elementName = record.get("elementName").toString()
    val elementDescription = record.get("elementDescription").toString()
    return new ElementDefinition(elementId, elementName, elementDescription)
  }

  private def createElement(graphDB:GraphDatabaseService,
    commandOptions: Map[String, AnyRef],
    elementDef: ElementDefinition
  ):Unit = {
    logger.debug("CreateElement: Creating data set.")
    commandOptions.+=("element_description"->elementDef.description)
    val exclude = List("dsId", "dsName", "edId", "mid")
    val prefix = "e"
    val fields:String = buildSetClause(prefix, commandOptions.toMap, exclude)

    val statement = """
      |merge(e:label
      |{
      |  mid:{mid}
      |})
      |on create set fields, e.creation_time = timestamp()
      |on match set e.last_modified_time = timestamp()
      """.stripMargin
        .replaceAll("label", elementDef.name)
        .replaceAll("fields", fields)

    run( graphDB,
      statement,
      commandOptions,
      emptyResultProcessor[DataSet])
  }

  private def registerElement(graphDB:GraphDatabaseService,
    commandOptions: Map[String, AnyRef],
    elementDef: ElementDefinition
  ):Unit = {
    logger.debug("CreateElement: Associating the new element to the dataset.")
    val associateToDataSet = """
      |match (e:label) where e.mid = {mid}
      |match (ds:internal_data_set) where ds.mid = {dsId}
      |merge (ds)-[:contains]->(e)
      """.stripMargin.replaceAll("label", elementDef.name)
      run(graphDB,
        associateToDataSet,
        commandOptions,
        emptyResultProcessor[DataSet])
  }
}
