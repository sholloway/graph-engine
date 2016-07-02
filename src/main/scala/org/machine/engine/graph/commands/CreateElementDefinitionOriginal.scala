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

/*Deprecated, should not be used.*/
class CreateElementDefintionOriginal(database: GraphDatabaseService,
  cmdScope: CommandScope,
  cmdOptions: GraphCommandOptions) extends Neo4InsertCommand[String] with LazyLogging{
  import Neo4JHelper._

  def execute():InsertCmdResult[String] = {
    logger.debug("CreateElementDefintion: Executing Command")
    generateId(cmdOptions)
    /*
    FIXME Merge of an ED needs to consider the scope.
    Rather than using the pattern of creating a node, then Associating
    it with a scope item, the initial merge needs to consider that.
    (scope)-[:contains]->(ed)
    */
    transaction(database, (graphDB: GraphDatabaseService) => {
      createElementDefinition(graphDB)
      createPropertyDefinitions(graphDB)
      registerTheElement(graphDB)
    })
    return InsertCmdResult(cmdOptions.option[String]("mid"))
  }

  private def createElementDefinition(graphDB: GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Creating element definition.")
    val createElementDefinitionStatement = """
      |merge(ed:element_definition
      |{
      |  name:{name}
      |})
      |on create set ed.mid = {mid}
      |on create set ed.description = {description}
      |on create set ed.creation_time = timestamp()
      |on match set ed.last_modified_time = timestamp()
      """.stripMargin

    run( graphDB,
      createElementDefinitionStatement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[ElementDefinition])
  }

  private def createPropertyDefinitions(graphDB: GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Creating property definitions.")
    val createPropertyStatement = """
      |merge(pd:property_definition
      |{
      |  mid:{mid},
      |  name:{name},
      |  type:{type},
      |  description:{description}
      |})
      |on create set pd.creation_time = timestamp()
      |on match set pd.last_modified_time = timestamp()
      """.stripMargin

    val createAssoicationStatement = """
      |match (ed:element_definition) where ed.mid = {elementId}
      |match (pd:property_definition) where pd.mid = {propertyId}
      |merge (ed)-[:composed_of]->(pd)
      """.stripMargin

    val properties = cmdOptions.option[PropertyDefinitions]("properties")
    val mid = cmdOptions.option[String]("mid")
    properties.toList.foreach(property => {
      run( graphDB,
        createPropertyStatement,
        property.toMap,
        emptyResultProcessor[ElementDefinition])

      run( graphDB,
        createAssoicationStatement,
        Map("elementId" -> mid,
          "propertyId" -> property.id),
        emptyResultProcessor[ElementDefinition])
    })
  }

  /*
  The challenge: ElementDefintions can be provisioned in SystemSpace,
  UserSpace and DataSets which are a subset in UserSpace.
  */
  private def registerTheElement(graphDB: GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Associating the element definition to the system space.")
    val statement = matchRegisterStatement()
    run(graphDB,
      statement,
      cmdOptions.toJavaMap,
      emptyResultProcessor[ElementDefinition])
  }

  private def matchRegisterStatement(): String = {
    val query = cmdScope match {
      case CommandScopes.SystemSpaceScope => {
        """
          |match (ss:label)
          |match (ed:element_definition) where ed.mid = {mid}
          |merge (ss)-[:exists_in]->(ed)
          """.stripMargin.replaceAll("label", cmdScope.scope)
      }
      case CommandScopes.UserSpaceScope => {
        """
          |match (us:label)
          |match (ed:element_definition) where ed.mid = {mid}
          |merge (us)-[:exists_in]->(ed)
          """.stripMargin.replaceAll("label", cmdScope.scope)
      }
      case CommandScopes.DataSetScope => {
        val filter = buildDataSetFilter(cmdOptions)
        """
        |match (ds:label) dsFilter
        |match (ed:element_definition) where ed.mid = {mid}
        |merge (ds)-[:exists_in]->(ed)
        """
        .stripMargin
        .replaceAll("label", cmdScope.scope)
        .replaceAll("dsFilter", filter)
      }
      case _ => throw new InternalErrorException("No Matching Scope Found: "+cmdScope)
    }
    return query
  }

  private def buildDataSetFilter(cmdOptions: GraphCommandOptions):String = {
    var filter:String = null
    if(cmdOptions.contains("dsId")){
      filter = "where ds.mid = {dsId}"
    }else if(cmdOptions.contains("dsName")){
      filter = "where ds.name = {dsName}"
    }else{
      val msg = """
      |CreateElementDefintion requires that dsId or dsName is provided on
      |commandOptions when the scope is of type CommandScopes.DataSet.
      """.stripMargin
      throw new InternalErrorException(msg)
    }
    return filter
  }
}
