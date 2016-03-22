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

class CreateElement(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions,
  logger: Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("CreateElement: Executing Command")

    val elementDef = findElementDefinition(database, cmdOptions)

    transaction(database, (graphDB:GraphDatabaseService) => {
      createElement(graphDB, cmdOptions, elementDef)
      registerElement(graphDB, cmdOptions, elementDef)
    })
    return cmdOptions.field[String]("mid")
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

  // private def createElement(graphDB:GraphDatabaseService,
  //   options: Map[String, AnyRef],
  //   elementDef: ElementDefinition
  // ):Unit = {
  //   logger.debug("CreateElement: Creating data set.")
  //   options.+=("element_description" -> elementDef.description)
  //   val exclude = List("dsId", "dsName", "edId", "mid")
  //   val prefix = "e"
  //   val fields:String = buildSetClause(prefix, options.toMap, exclude)
  //
  //   val statement = """
  //     |merge(e:label
  //     |{
  //     |  mid:{mid}
  //     |})
  //     |on create set fields, e.creation_time = timestamp()
  //     |on match set e.last_modified_time = timestamp()
  //     """.stripMargin
  //       .replaceAll("label", elementDef.name)
  //       .replaceAll("fields", fields)
  //
  //   run( graphDB,
  //     statement,
  //     options,
  //     emptyResultProcessor[DataSet])
  // }

  //BUG: Need to set creation_time. Look at Neo4J code and verify how they are calculating it.
  //This is important since, only elements will use this and the other graph elements are using timestamp()
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
      |match (ds:internal_data_set) where ds.mid = {dsId}
      |create (ds)-[:contains]->(e)
      """.stripMargin.replaceAll("label", elementDef.name)
      run(graphDB,
        associateToDataSet,
        commandOptions.toJavaMap,
        emptyResultProcessor[DataSet])
  }
}
