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

class AssociateElements(database:GraphDatabaseService,
  cmdScope:CommandScope,
  cmdOptions:GraphCommandOptions) extends Neo4JCommand with LazyLogging{
  import Neo4JHelper._

  val expectedElements = List("dsId", "startingElementId", "endingElementId",
    "associationName", "associationId")

  def execute():String = {
    logger.debug("CreateDataSet: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      associateElements(graphDB)
    })
    return cmdOptions.option[String]("associationId")
  }

  /*
  Notes:
  1. Make sure that the elements are both in the same dataset.
  2. associationName is optional. If it is not provided, then the default association
      should be :is_associated_with
  */
  private def associateElements(graphDB:GraphDatabaseService):Unit = {
    logger.debug("AssociateElements: Associating elements.")
    val associationType = buildAssociationLabel(cmdOptions)
    val exclude = List("dsId", "startingElementId", "endingElementId", "associationName")
    cmdOptions.addOption("association_time", time)
    val setClause = buildRelationshipClause(cmdOptions.keys, exclude)
    val statement = """
      |match (ds:internal_data_set {mid:{dsId}})-[:contains]->(startingElement {mid:{startingElementId}})
      |match (ds:internal_data_set {mid:{dsId}})-[:contains]->(endingElement {mid:{endingElementId}})
      |create (startingElement)-[a:association {setClause}]->(endingElement)
      """.stripMargin
        .replaceAll("association", associationType)
        .replaceAll("setClause", setClause)

    logger.debug("AssociateElements: Generated creation statement.")
    logger.debug(statement)

    run( graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[Association])
  }

  private def buildAssociationLabel(cmdOptions:GraphCommandOptions):String = {
    var associationLabel:String = null
    if (cmdOptions.contains("associationName")){
      associationLabel = cmdOptions.option[String]("associationName")
    } else{
      associationLabel = "is_associated_with"
    }
    return associationLabel
  }
}
