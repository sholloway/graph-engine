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

/*
FIXME: Element Definitions that contain spaces cannot be uses.
FIXME: Rewrite to use partial functions and reduce the number of transactions.
FIXME: Account for failure to create. (e.g. Bad dataset, bad edId.)
*/
class CreateElement(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateElement: Executing Command")
    generateId(cmdOptions)
    val elementDef = findElementDefinition(database, cmdOptions)
    transaction(database, (graphDB:GraphDatabaseService) => {
      createElement(graphDB, cmdOptions, elementDef)
      registerElement(graphDB, cmdOptions, elementDef)
    })
    return InsertCmdResult(cmdOptions.field[String]("mid"))
  }

  /*
  Find the required ElementDefinition. This could be in system space, user space or the dataset.
  */
  private def findElementDefinition(graphDB: GraphDatabaseService, cmdOptions: GraphCommandOptions):ElementDefinition = {
    val statement = """
    |match (ed:element_definition) where ed.mid = {edId}
    |return ed.mid as elementId,
    |  ed.name as elementName,
    |  ed.description as elementDescription
    """.stripMargin

    val elementDefs = query[ElementDefinition](database,
      statement, cmdOptions.toJavaMap, mapElementDefs)
    val edId = cmdOptions.field[String]("edId")
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
    cmdOptions: GraphCommandOptions,
    elementDef: ElementDefinition
  ):Unit = {
    logger.debug("CreateElement: Creating data set.")
    cmdOptions.addOption("element_description", elementDef.description)
    cmdOptions.addOption("creation_time", time)
    val exclude = List("dsId", "dsName", "edId", "elementId")

    transaction(graphDB, (tx: GraphDatabaseService) => {
      val node = tx.createNode(DynamicLabel.label(elementDef.name))
      node.addLabel(DynamicLabel.label("element")) //TODO Pull into enum.
      cmdOptions.foreach(field => {
        if (!exclude.contains(field._1)){
          node.setProperty(field._1, field._2)
        }
      })
    })
  }

  private def registerElement(graphDB: GraphDatabaseService,
    commandOptions: GraphCommandOptions,
    elementDef: ElementDefinition
  ):Unit = {
    logger.debug("CreateElement: Associating the new element to the dataset.")
    val associateToDataSet = """
      |match (e:label) where e.mid = {mid}
      |match (ds:data_set) where ds.mid = {dsId}
      |create (ds)-[:contains]->(e)
      """.stripMargin.replaceAll("label", elementDef.name)
      run(graphDB,
        associateToDataSet,
        commandOptions.toJavaMap,
        emptyResultProcessor[DataSet])
  }
}
