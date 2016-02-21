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
  scope:CommandScope,
  commandOptions:Map[String, AnyRef],
  logger:Logger) extends Neo4JCommand{
  import Neo4JHelper._

  def execute() = {
    logger.debug("CreateElementDefintion: Executing Command")
    transaction(database, (graphDB:GraphDatabaseService) => {
      createElementDefinition(graphDB)
      createPropertyDefinitions(graphDB)
      associateTheElementToSystemSpace(graphDB)
    })
  }

  private def emptyResultProcessor(results: ArrayBuffer[UserSpace],
    record: java.util.Map[java.lang.String, Object]) = { }

  private def createElementDefinition(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Creating element definition.")
    val createElementDefinitionStatement = """
      |merge(ed:element_definition
      |  {mid:{mid},
      |  name:{name},
      |  description:{description}
      |})
      |on create set ed.creation_time = timestamp()
      |on match set ed.last_modified_time = timestamp()
      """.stripMargin

    insert( graphDB,
      createElementDefinitionStatement,
      commandOptions,
      emptyResultProcessor)
  }

  private def createPropertyDefinitions(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Creating property definitions.")
    val createPropertyStatement = """
      |merge(pd:property_definition
      |  {mid:{mid},
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
      insert( graphDB,
        createPropertyStatement,
        property,
        emptyResultProcessor)

      insert( graphDB,
        createAssoicationStatement,
        Map("elementId" -> commandOptions("mid"),
          "propertyId" -> property("mid")),
        emptyResultProcessor)
    })
  }

  private def associateTheElementToSystemSpace(graphDB:GraphDatabaseService):Unit = {
    logger.debug("CreateElementDefintion: Associating the element definition to the system space.")
    val associateToSystemSpace = """
      |match (ss:internal_system_space)
      |match (ed:element_definition) where ed.mid = {elementId}
      |merge (ss)-[:exists_in]->(ed)
      """.stripMargin
      insert(graphDB,
        associateToSystemSpace,
        Map("elementId" -> commandOptions("mid")),
        emptyResultProcessor)
  }
}
