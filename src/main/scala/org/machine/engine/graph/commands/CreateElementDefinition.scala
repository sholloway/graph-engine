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

class CreateElementDefintion(database:GraphDatabaseService,
  cmdScope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute():String = {
    logger.debug("CreateElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      createElementDefinition(graphDB)
      createPropertyDefinitions(graphDB)
      registerTheElement(graphDB)
    })
    val mid = commandOptions.get("mid").getOrElse(throw new InternalErrorException("mid required"))
    return mid.toString
  }

  private def createElementDefinition(graphDB:GraphDatabaseService):Unit = {
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
      commandOptions,
      emptyResultProcessor[ElementDefinition])
  }

  private def createPropertyDefinitions(graphDB:GraphDatabaseService):Unit = {
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

    commandOptions("properties").asInstanceOf[ListBuffer[Map[String, AnyRef]]].foreach(property => {
      run( graphDB,
        createPropertyStatement,
        property,
        emptyResultProcessor[ElementDefinition])

      run( graphDB,
        createAssoicationStatement,
        Map("elementId" -> commandOptions("mid"),
          "propertyId" -> property("mid")),
        emptyResultProcessor[ElementDefinition])
    })
  }

  /*
  The challenge: ElementDefintions can be provisioned in SystemSpace,
  UserSpace and DataSets which are a subset in UserSpace.
  */
  private def registerTheElement(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Associating the element definition to the system space.")
    val statement = matchRegisterStatement()
    run(graphDB,
      statement,
      commandOptions,
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
        val filter = buildDataSetFilter(commandOptions)
        """
        |match (ds:label) dsFilter
        |match (ed:element_definition) where ed.mid = {mid}
        |merge (ds)-[:exists_in]->(ed)
        """
        .stripMargin
        .replaceAll("label", cmdScope.scope)
        .replaceAll("dsFilter", filter)
      }
    }
    return query
  }

  private def buildDataSetFilter(options:Map[String, AnyRef]):String = {
    var filter:String = null
    if(options.contains("dsId")){
      filter = "where ds.mid = {dsId}"
    }else if(options.contains("dsName")){
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
